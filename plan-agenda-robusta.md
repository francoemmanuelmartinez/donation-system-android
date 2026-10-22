# Plan — Agenda robusta (modelo estándar de agendamiento)

**Fecha:** 2026-10-22
**Estado:** implementado (T1–T10 completos; suite 68/68 en verde; docs actualizadas).
**Alcance:** robustecer la Agenda a un modelo estándar mínimo — ciclo de vida con auditoría, CRUD completo (alta, edición, cancelación, reagendado), asistencia (`ATTENDED`/`NO_SHOW`), validaciones de negocio y listado por estado. El reagendado deja de ser un caso aislado: pasa a ser una transición del ciclo de vida.

> Este plan reemplaza a `plan-reagendar-turno.md` (v1). Se consolida en un solo documento para no dejar dos guías del mismo tema.

## Objetivo
Que la Agenda deje de ser una lista de turnos y pase a reflejar el ciclo real: se agenda → se edita/reagenda → se cancela o se atiende (o no se presenta). Con trazabilidad de quién/cuándo/por qué y con validaciones del lado del backend (hoy el cliente es la única barrera).

## Decisiones resueltas
- **D1 — Listado:** la Agenda muestra por defecto solo `SCHEDULED` (con opción "ver todos" / histórico).
- **D2 — `center_id` del reagendado:** turno viejo → donante → `1` como fallback.
- **D3 — Elegibilidad:** avisar (no bloquear) si la fecha es anterior a `nextEligibleDate`.
- **D4 — Fetch del donante falla en modo reagendar:** permitir la operación sin el aviso de elegibilidad (nota discreta).
- **D5 — Fechas permitidas:** cualquier fecha ≥ hoy (no exige estrictamente "posterior" al turno actual).
- **D6 — Entrada UI:** menú overflow (⋮) en la card, no un tercer ícono fijo.
- **D7 — Trazabilidad viejo↔nuevo:** se implementa en P1 como `rescheduled_to_appointment_id` (columna nullable) + `cancel_reason='RESCHEDULED'`. No se agrega un estado `RESCHEDULED` para no tocar todos los filtros.
- **D8 — Cancelación:** solo se cancela/reagenda un turno `SCHEDULED`. `ATTENDED` y `NO_SHOW` quedan protegidos.
- **D9 — Asistencia:** al registrar una donación se linkea `donations.appointment_id` y se marca el turno `ATTENDED` (misma transacción).
- **D10 — No-show:** estado `NO_SHOW` manual desde la Agenda (solo turnos pasados `SCHEDULED`).
- **D11 — Edición:** `update` de turno (cambiar fecha/hora/procedimiento sin cancelar) además de reagendar.
- **D12 — Motivo de cancelación:** opcional (`cancel_reason`), sin obligatoriedad.
- **D13 — Listado activo vs. histórico (UI/UX):** la Agenda operativa muestra en el listado normal **solo turnos activos** (`SCHEDULED`). Los `CANCELLED`/`ATTENDED`/`NO_SHOW` salen del activo y se ven en el **histórico** ("Ver todos"). Tras cancelar/reagendar/editar, un `Snackbar` confirma y ofrece acceso al histórico (+ "Deshacer" en cancelar/editar). Si el día queda sin activos pero hay histórico, el estado vacío muestra "No quedan turnos activos" + botón "Ver histórico".
- **D14 — Ubicación del control de estado:** chip de estado **solo en el histórico** (en el listado activo no hay chip, porque todo es `SCHEDULED`).
- **D15 — Etiqueta visual "Reagendado":** el turno viejo de un reagendado se guarda como `CANCELLED` (estado lógico) pero la UI muestra el chip **"Reagendado"** cuando `cancel_reason='RESCHEDULED'` (derivado, sin tocar el ENUM ni los filtros). Regla de etiquetado en la app: `CANCELLED`+`RESCHEDULED`→"Reagendado"; otro `CANCELLED`→"Cancelado"; `ATTENDED`→"Atendido"; `NO_SHOW`→"No vino".
- **D16 — "Ver todos":** botón de **texto** "Ver todos" en el header de la semana (junto a los íconos de calendario y dispatch), no un ícono ni un footer.
- **D17 — Aviso por WhatsApp (fase A):** al cancelar/reagendar, enviar un mensaje **de texto libre** al donante vía `whatsapp_sender.php` (después del COMMIT). *Limitación:* fuera de la ventana de 24 h de WhatsApp puede no entregarse; la entrega garantizada (plantilla UTILITY) y los botones/poll quedan en `plan-recordatorios-interactivos.md`.
- **D18 — Solicitud de reagendado del donante (fase D):** el donante puede pedir "reagendar" (no elegir fecha); se marca el turno activo con `reschedule_requested_at` y la Agenda lo muestra en un modo "Solicitudes" con chip **"Pidió reagendar"**; el operador lo reagenda manualmente y la marca se limpia. Detalle del canal interactivo en el plan de recordatorios.
- **Fuera de P1:** capacidad por franja, recursos/sillones, feriados, recurrencia, waitlist, enforcement de roles en backend.

## Modelo objetivo
### Estados
`SCHEDULED → ATTENDED` (donación registrada) · `SCHEDULED → NO_SHOW` (manual, turno pasado) · `SCHEDULED → CANCELLED` (cancelación manual) · `SCHEDULED → CANCELLED` + nuevo `SCHEDULED` (reagendado, con link viejo↔nuevo).

### Campos nuevos
| Tabla | Campo | Uso |
|---|---|---|
| `appointments` | `cancelled_at`, `cancelled_by_user_id`, `cancel_reason` | auditoría de cancelación/reagendado |
| `appointments` | `rescheduled_to_appointment_id` | trazabilidad viejo↔nuevo (D7) |
| `appointments` | `updated_at` | última modificación |
| `appointments` | `status` amplía ENUM con `NO_SHOW` | D10 |
| `appointments` | `reschedule_requested_at` | solicitud de reagendado del donante (D18) |
| `donations` | `appointment_id` (nullable) | vínculo atención↔turno (D9) |

### Reglas de transición
1. Solo `SCHEDULED` se cancela, edita o reagenda.
2. Cancelar/reagendar es **soft** (`status='CANCELLED'`), nunca DELETE.
3. Reagendar = cancelar viejo + crear nuevo, **en una transacción** (cancel-first por el chequeo de 2º turno/día).
4. Un turno `ATTENDED` no se puede cancelar ni reagendar.
5. No se puede agendar/editar a fecha pasada (validación backend).
6. La Agenda no muestra `CANCELLED`/`ATTENDED`/`NO_SHOW` salvo que se pida el histórico (D13).
7. En el histórico, el chip de estado se deriva (D15); en el activo no hay chip (todos `SCHEDULED`).

## Contexto técnico relevante
- `appointments.status ENUM('SCHEDULED','ATTENDED','CANCELLED')` (`sql/donation_system.sql:230`); `ATTENDED` **nunca** se setea hoy.
- `appointments.php:64` arma el SELECT de semana/mes; `:74` y `:84` **no filtran** por estado → los cancelados siguen apareciendo y engordan los conteos del calendario.
- `appointments.php:178-190` valida formato de fecha/hora/procedimiento, pero **no** rechaza fechas pasadas; `:202` bloquea 2º turno el mismo día (`status <> 'CANCELLED'`); `:208` inserta con `center_id`.
- `restrictions.php:98` `cancelAppointment()` cancela cualquier estado ≠ `CANCELLED` (incluye `ATTENDED`); `:113` `validateAppointmentPayload()`; `:128` `createAppointmentRow()` inserta **sin** `center_id` → NULL → invisible bajo filtro de centro; `:150` `handleReschedule()` (cancel + create, sin transacción explícita).
- `restrictions.php:256-257` resuelve `centerId` del donante (fallback 1) — reutilizable.
- `backend/donations.php:124` inserta la donación **sin** vínculo al turno ni actualización de estado.
- `send_reminders.php:194` filtra `status='SCHEDULED'` → un turno cancelado no recibe recordatorio.
- `statistics.php` no consulta `appointments` → sin doble conteo.
- App: `ScheduleAppointmentFragment` (búsqueda por DNI → fecha → hora libre con `TimePickerDialog` en `:231` → procedimiento → confirmar). `AppointmentsFragment.onResume()` (`:113`) ya recarga la semana. `MainActivity.openScheduleAppointment()` (`:82`) + `push()`. La Agenda abre el formulario clínico con `selectedAppointment` vía `SharedDonorViewModel`.
- `Appointment` (modelo Android) ya expone `status`, pero la UI no lo usa.

## Correcciones al plan v1 (obligatorias)
1. **Filtro de estado:** agregar `AND a.status = 'SCHEDULED'` a las dos ramas GET (`appointments.php:74`, `:84`); opción `status=all|<estado>`.
2. **Refresh entre fragments:** no usar `rescheduleResult` cross-fragment (cada `ViewModelProvider(this)` es otra instancia). Navegar con `MainActivity.push(...)` y refrescar con `AppointmentsFragment.onResume()`.
3. **`nextEligibleDate`:** el `Appointment` no lo trae; en modo reagendar cargar el `Donor` con `DonorsViewModel.getDonorById` (`ApiService:70`, `DonorRepository:32`, `DonorsViewModel:99`) y desacoplar `showConfirmation()`/`isBeforeRecoveryEnd()` de `SearchResultItem`.
4. **`center_id`:** `createAppointmentRow()` debe recibir y persistir `center_id` (turno viejo → donante → 1).

## Fases
- **P1 (este plan):** esquema + lifecycle/auditoría + CRUD + asistencia + validaciones + filtro de Agenda.
- **P2 (futuro):** concurrencia/idempotencia, búsqueda/filtros/paginación, notificación de cambio al donante.
- **P3 (futuro):** capacidad por franja, recursos/sillones, feriados/cierres, recurrencia, waitlist.

## Tickets P1

### T1 — Esquema y sincronización
**Bloqueado por:** — (raíz)
**Archivos:** `sql/migration_appointments_lifecycle.sql` (nuevo), `sql/donation_system.sql`, `scripts/sync-db.ps1` (solo si hace falta).
**Tareas:**
1. `ALTER TABLE appointments`: ampliar ENUM con `NO_SHOW`; agregar `cancelled_at`, `cancelled_by_user_id`, `cancel_reason`, `rescheduled_to_appointment_id`, `reschedule_requested_at`, `updated_at`; FKs `cancelled_by_user_id → users(id)` y `rescheduled_to_appointment_id → appointments(id) ON DELETE SET NULL`.
2. `ALTER TABLE donations`: agregar `appointment_id` nullable + FK `ON DELETE SET NULL`.
3. Índice `idx_appointments_center_status_date (center_id, status, appointment_date)`.
4. Reflejar todo en `sql/donation_system.sql` y aplicar con `scripts\sync-db.ps1 -Push`.
**Aceptación:** migración idempotente; `sync-db.ps1` reconstruye sin errores; la app sigue funcionando sin cambios.

### T2 — Backend: librería compartida de lifecycle
**Bloqueado por:** T1
**Archivos:** `backend/appointments_lib.php` (nuevo), `backend/restrictions.php`.
**Tareas:**
1. Extraer `validateAppointmentPayload()`, `createAppointmentRow()`, `cancelAppointment()`, `handleReschedule()` desde `restrictions.php` (mismos contratos) y `require_once` en ambos.
2. `validateAppointmentPayload()`: agregar rechazo de fecha pasada (comparar contra `date('Y-m-d')`) y mantener validación de hora/procedimiento.
3. `createAppointmentRow($conn, $donorId, $date, $time, $procedure, $centerId)`: persistir `center_id` (fix #4) y recibir el `centerId` resuelto.
4. `cancelAppointment($conn, $appointmentId, $donorId, $reason = null, $userId = null, $rescheduleToId = null)`: solo `SCHEDULED` (D8); setear `status`, `cancelled_at`, `cancelled_by_user_id`, `cancel_reason`, `rescheduled_to_appointment_id`; idempotente si ya está `CANCELLED`.
5. Helper `markAttendance($conn, $appointmentId, $donorId, $status)` para `ATTENDED`/`NO_SHOW` (solo `SCHEDULED`).
**Aceptación:** `restrictions.php` conserva su comportamiento observable; `php -l` limpio.

### T3 — Backend: reagendado atómico
**Bloqueado por:** T2
**Archivos:** `backend/appointments.php`.
**Tareas:**
1. `action=reschedule` (POST): validar payload; `BEGIN`; SELECT del turno por `id`+`donor_id` con `status='SCHEDULED'` (404/409 si no); resolver `center_id` (D2); cancelar viejo con `cancel_reason='RESCHEDULED'`; INSERT nuevo con `center_id`; UPDATE `rescheduled_to_appointment_id`; `COMMIT` (rollback en error).
2. Respuesta `{ cancelledAppointmentId, appointmentId, appointmentDate, appointmentTime, targetProcedure }`.
**Aceptación:** reagendar mismo día a otra hora no choca; 404 turno ajeno/inexistente; 400 payload; 409 si quedara otro turno ese día; rollback ante falla del INSERT.

### T4 — Backend: edición, cancelación, no-show y asistencia
**Bloqueado por:** T2
**Archivos:** `backend/appointments.php`, `backend/donations.php`.
**Tareas:**
1. `action=update_appointment`: cambiar fecha/hora/procedimiento de un `SCHEDULED` (mismas validaciones y chequeo de 2º turno/día, excluyendo el propio turno).
2. `action=cancel_appointment`: `appointmentId` + `cancelReason` opcional (D12) + usuario de sesión si está disponible.
3. `action=mark_attendance`: `ATTENDED`|`NO_SHOW`, solo `SCHEDULED` y solo fecha ≤ hoy.
4. `donations.php`: aceptar `appointmentId` opcional; validar que pertenece al donante y está `SCHEDULED`; en la misma transacción insertar la donación con `appointment_id` y marcar el turno `ATTENDED` (D9).
**Aceptación:** `ATTENDED`/`NO_SHOW`/`CANCELLED` no se pueden re-cancelar ni reagendar; una donación con `appointmentId` ajeno se rechaza sin insertar nada.

### T5 — App: capa de datos
**Bloqueado por:** T3, T4
**Archivos:** `models/requests/RescheduleAppointmentRequest.java`, `CancelAppointmentRequest.java`, `UpdateAppointmentRequest.java`, `MarkAttendanceRequest.java`; `api/ApiService.java`; `repositories/AppointmentRepository.java`; `viewmodels/AppointmentsViewModel.java`.
**Tareas:** requests con `@SerializedName`; endpoints `reschedule/update/cancel/mark_attendance`; métodos en repository; `MutableLiveData<Resource<...>>` por operación en el ViewModel (mismo patrón que `createAppointment`).
**Aceptación:** compila; cada operación postea success/error uniforme.

### T6 — App: `ScheduleAppointmentFragment` en modos editar/reagendar
**Bloqueado por:** T5
**Archivos:** `views/fragments/ScheduleAppointmentFragment.java`, `res/layout/fragment_schedule_appointment.xml`, `res/values/strings.xml`.
**Tareas:**
1. Args: `donorId`, `donorName`, `donorDni`, `appointmentId`, `currentDate`, `currentTime`, `currentProcedure`, `mode` (`EDIT`|`RESCHEDULE`).
2. Ocultar búsqueda por DNI; mostrar card del turno actual; precargar procedimiento y fecha/hora.
3. Cargar `Donor` con `getDonorById` para elegibilidad; si falla, permitir sin aviso (D4).
4. Confirmar → `update` o `reschedule` según modo; títulos/botones adaptados.
**Aceptación:** no pide DNI; muestra el turno actual; éxito → `popBackStack` + toast; falla de fetch no bloquea.

### T7 — App: acciones en la Agenda
**Bloqueado por:** T6
**Archivos:** `res/layout/item_appointment.xml`, `res/menu/menu_appointment_actions.xml` (nuevo), `adapters/AppointmentsAdapter.java`, `views/fragments/AppointmentsFragment.java`, `views/MainActivity.java`, `res/values/strings.xml`.
**Tareas:**
1. Overflow (⋮) con `PopupMenu`: **Reagendar**, **Editar**, **Cancelar**, **Marcar no vino** (este último solo si `SCHEDULED` y fecha ≤ hoy).
2. Listeners en el adapter; `AppointmentsFragment` arma args y navega con `push()`.
3. Cancelar: diálogo con motivo opcional; No-show: confirmación simple.
4. Tras la operación, refrescar vía `onResume()` (fix #2), no por LiveData cross-fragment. Confirmar con `Snackbar` y atajo al histórico (+ "Deshacer" en cancelar/editar, D13).
**Aceptación:** cada acción abre el flujo correcto; al volver, la lista refleja el cambio y confirma con snackbar.

### T7b — Backend: aviso por WhatsApp al donante (fase A)
**Bloqueado por:** T3, T4
**Archivos:** `backend/appointments.php`, `backend/whatsapp_sender.php` (reutiliza `sendViaOpenWa`).
**Tareas:**
1. Tras el COMMIT de `cancel_appointment`/`reschedule`, enviar mensaje de texto libre al donante (teléfono primario) con el cambio. No revertir el turno si el envío falla.
2. Registrar el resultado del envío en la respuesta (ej. `whatsappNotified: true|false`) sin bloquear el flujo.
**Aceptación:** cancelar/reagendar dispara un mensaje; si OpenWA no está o falla, la operación igual se completa.
**Nota:** entrega garantizada (plantilla UTILITY) fuera de alcance → `plan-recordatorios-interactivos.md`.

### T8 — App: Agenda activa vs. histórico
**Bloqueado por:** T7
**Archivos:** `views/fragments/AppointmentsFragment.java`, `adapters/AppointmentsAdapter.java`, `item_appointment.xml`, `res/layout/fragment_appointments.xml`, `res/values/strings.xml`.
**Tareas:**
1. Listado normal: solo `SCHEDULED` (D1/D13). Botón de texto **"Ver todos"** en el header de la semana (D16) que alterna a histórico.
2. Histórico: muestra `CANCELLED`/`ATTENDED`/`NO_SHOW` con chip de estado derivado (D14/D15): `CANCELLED`+`RESCHEDULED`→"Reagendado"; otro `CANCELLED`→"Cancelado"; `ATTENDED`→"Atendido"; `NO_SHOW`→"No vino". El chip solo aparece en histórico.
3. Estado vacío con histórico disponible: "No quedan turnos activos" + botón "Ver histórico" (D13).
4. El calendario mensual cuenta solo `SCHEDULED`.
**Aceptación:** un turno cancelado/atendido/no-vino no aparece en el activo ni suma en el calendario; en histórico muestra su chip (incluido "Reagendado" para los movidos).

### T10 — App/Backend: solicitud de reagendado del donante (fase D)
**Bloqueado por:** T3, T8
**Archivos:** `backend/appointments.php` (leer/limpiar `reschedule_requested_at`), `views/fragments/AppointmentsFragment.java`, `adapters/AppointmentsAdapter.java`, `item_appointment.xml`.
**Tareas:**
1. La Agenda reconoce `reschedule_requested_at` y ofrece un modo **"Solicitudes"** que lista esos turnos primero.
2. La card muestra el chip **"Pidió reagendar"** (turno sigue `SCHEDULED` y activo).
3. El operador reagenda con el flujo de T6/T7; al confirmar se limpia `reschedule_requested_at`.
4. El origen de la marca (poll/webhook de WhatsApp) se especifica en `plan-recordatorios-interactivos.md`; esta tarea funciona aunque la marca se cargue manualmente para pruebas.
**Aceptación:** un turno marcado aparece en "Solicitudes" con su chip; tras reagendar, la marca desaparece.

### T9 — Documentación y tests
**Bloqueado por:** T3, T4 (tests), T8, T10 (QA)
**Archivos:** `TESTING.md`/tests de integración, `wiki-design-specification.md`, `technical_summary.md`, `data-dictionary.md`, `AGENTS.md` (si cambia el esquema).
**Tareas:**
1. Tests de integración (BD `donation_system_test`) para reschedule/update/cancel/mark_attendance, link de donación y aviso WhatsApp (mock/skip si OpenWA no está).
2. Documentar estados, transiciones, columnas nuevas y la etiqueta visual "Reagendado".
3. QA manual del flujo completo (activo vs. histórico, chips, "Ver todos", solicitud de reagendado).
**Aceptación:** suite en verde; docs actualizadas; `check-encoding.ps1` OK.

## Verificación
1. `php -l` de cada PHP tocado.
2. `scripts\sync-db.ps1 -Push` (T1) sin errores.
3. `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
4. `powershell -ExecutionPolicy Bypass -File .\scripts\check-encoding.ps1 -Quiet` → OK.
5. QA manual: agendar → editar → reagendar (mismo día y otro día) → cancelar con motivo → marcar no-show → registrar donación y ver el turno `ATTENDED`; verificar que un turno cerrado sale del activo y solo aparece en histórico con su chip (incluido "Reagendado"); verificar el aviso por WhatsApp y el modo "Solicitudes".

## Fuera de alcance
- Capacidad por franja, recursos/sillones, feriados/cierres, recurrencia, waitlist (P3).
- Concurrencia/idempotencia avanzada y búsqueda/paginación (P2).
- Enforcement de roles en backend (frente separado; hoy el gate es del cliente).
- Botones/poll interactivos de WhatsApp y entrega garantizada (plantilla UTILITY) → `plan-recordatorios-interactivos.md`.

## Riesgos
- **Mismo día:** sin cancel-first, el chequeo de 2º turno rechaza. Mitigación: transacción con UPDATE antes del INSERT.
- **Recordatorios:** la fila nueva arranca con flags en 0; el turno viejo `CANCELLED` no se envía (`send_reminders.php:194`).
- **Aviso WhatsApp:** fuera de la ventana de 24 h puede no entregarse; se documenta como limitación (D17) y se cubre con plantilla en `plan-recordatorios-interactivos.md`.
- **Migración destructiva:** el `ENUM` se amplía, no se reordena; las columnas son nullable. Respaldo previo con `sync-db.ps1 -Pull`.
- **Doble fuente de refresh:** evitar observers cross-fragment; usar `onResume()`.
- **Código duplicado:** toda la lógica de lifecycle en `appointments_lib.php`, no en los endpoints.
- **Chip derivado:** la etiqueta "Reagendado" depende de `cancel_reason`; si un cancelado manual usara ese motivo, se mostraría como reagendado. Mitigación: `RESCHEDULED` es un valor reservado que solo escribe el flujo de reagendado.
