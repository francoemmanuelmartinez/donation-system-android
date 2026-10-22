# Plan — Reagendar turno desde la Agenda

**Fecha:** 2026-10-21
**Estado:** propuesta, pendiente de aprobación (no implementado).
**Alcance:** acción de reagendado (cancelar el turno actual + crear uno para fecha posterior) disparada desde la Agenda. No cambia el alta de turnos ni el registro de donaciones.

## Objetivo
Permitir, desde la pantalla **Agenda** (`AppointmentsFragment`), reagendar el turno de un donante: cancelar el turno actual (`SCHEDULED`) y crear uno nuevo para una fecha/hora posterior, en **una sola operación atómica**, sin pasar por el editor de restricciones médicas.

## Decisiones resueltas
- Punto de entrada: **acción de fila "Reagendar"** en la card del turno (`item_appointment.xml`).
- UX: reutilizar `ScheduleAppointmentFragment` en **modo reagendar** (salta la búsqueda por DNI, muestra el turno actual, precarga el procedimiento).
- Backend: endpoint único `appointments.php?action=reschedule` que hace **cancel + create en una transacción**.
- Reutilización: extraer `handleReschedule()`/`createAppointmentRow()`/`validateAppointmentPayload()`/`cancelAppointment()` (hoy en `restrictions.php`) a un helper compartido; `restrictions.php` lo sigue usando para "veda + turno".
- Cancelación **soft** (`status='CANCELLED'`) para conservar historial.
- Trazabilidad del vínculo viejo↔nuevo: **opcional/diferida** (T5).

## Contexto técnico relevante
- `appointments.status ENUM('SCHEDULED','ATTENDED','CANCELLED')` (`sql/donation_system.sql:230`); cancelar = UPDATE a `CANCELLED`.
- `appointments.php:202` bloquea un 2º turno el mismo día (`status <> 'CANCELLED'`): el reschedule debe cancelar **antes** de crear (ese es el orden que ya usa `handleReschedule`, `restrictions.php:150`).
- Hoy **no hay** endpoint de reschedule en `appointments.php`; el único camino es `restrictions.php` (assign/update → `handleReschedule`), enterrado dentro del editor de restricciones.
- `ScheduleAppointmentFragment` hoy: buscar por DNI → fecha → hora (`TimePickerDialog` libre) → procedimiento (`toggleGroupScheduleProcedure`) → confirmar (`createAppointment`). Avisa si la fecha es anterior a `nextEligibleDate`.
- Navegación: `MainActivity.push(...)` / `openScheduleAppointment()`; la Agenda ya abre el formulario clínico con `selectedAppointment` vía `SharedDonorViewModel`.
- Los turnos arrastran `center_id` (multi-centro, Fase 4a).
- `send_reminders.php:194` filtra `status='SCHEDULED'` → un turno cancelado no recibe recordatorio.
- `statistics.php` no consulta `appointments` → sin riesgo de doble conteo.

## Tickets

### T1 — Backend: endpoint atómico de reagendado
**Bloqueado por:** — (raíz)
**Archivos:** `backend/appointments.php`, nuevo `backend/appointments_lib.php`, `backend/restrictions.php`.
**Tareas:**
1. Extraer `handleReschedule()`, `createAppointmentRow()`, `validateAppointmentPayload()`, `cancelAppointment()` de `restrictions.php` a `appointments_lib.php` (`require_once` en ambos).
2. Agregar `action=reschedule` (POST) en `appointments.php`:
   - Validar `appointmentId`, `donorId`, `appointmentDate`, `appointmentTime`, `targetProcedure` (`validateAppointmentPayload`).
   - `BEGIN`: SELECT del turno por `id`+`donor_id` (debe existir y estar `SCHEDULED`) → guardar su `center_id`.
   - UPDATE viejo → `CANCELLED`.
   - INSERT nuevo con el mismo `center_id`.
   - `COMMIT`.
   - Respuesta: `{ cancelledAppointmentId, appointmentId, appointmentDate, appointmentTime, targetProcedure }`.
3. `restrictions.php` pasa a usar el helper extraído (mismo contrato, sin cambios funcionales).
**Aceptación:** reagendar el mismo día a otra hora **no** choca (cancel-first); 404 si el turno no existe o no es del donante; 400 payload inválido; 409 si quedara otro turno ese día sin cancelar.

### T2 — App: capa de datos
**Bloqueado por:** T1
**Archivos:** `models/requests/RescheduleAppointmentRequest.java`, `api/ApiService.java`, `repositories/AppointmentRepository.java`, `viewmodels/AppointmentsViewModel.java`.
**Tareas:**
1. `RescheduleAppointmentRequest` con `@SerializedName` de `appointmentId`, `donorId`, `appointmentDate`, `appointmentTime`, `targetProcedure`.
2. `ApiService`: `@POST("appointments.php?action=reschedule") Call<ApiResponse<Map<String,Object>>> rescheduleAppointment(@Body RescheduleAppointmentRequest request);`
3. `AppointmentRepository.reschedule(...)` → `enqueue`.
4. `AppointmentsViewModel`: `MutableLiveData<Resource<Map<String,Object>>> rescheduleResult` + `rescheduleAppointment(...)` (mismo patrón que `createAppointment`).
**Aceptación:** compila; `rescheduleResult` postea success/error igual que el resto.

### T3 — App: `ScheduleAppointmentFragment` en modo reagendar
**Bloqueado por:** T2
**Archivos:** `views/fragments/ScheduleAppointmentFragment.java`, `res/layout/fragment_schedule_appointment.xml`, `res/values/strings.xml`.
**Tareas:**
1. Args (`Bundle`/`newInstance`): `donorId`, `donorName`, `donorDni`, `appointmentId`, `currentDate`, `currentTime`, `currentProcedure`.
2. En modo reagendar: ocultar la búsqueda por DNI; mostrar una card con el **turno actual**; precargar el procedimiento; mantener el selector libre de fecha/hora.
3. Al confirmar → `rescheduleAppointment(...)` en lugar de `createAppointment`.
4. Conservar el aviso de elegibilidad (`nextEligibleDate`) porque mover antes/después de la recuperación importa.
5. Título/botón adaptados ("Reagendar turno" / "Confirmar nuevo turno").
**Aceptación:** con args no pide DNI; muestra el turno actual; éxito → `popBackStack` + toast; error → toast.

### T4 — App: acción "Reagendar" en la Agenda
**Bloqueado por:** T3
**Archivos:** `res/layout/item_appointment.xml`, `adapters/AppointmentsAdapter.java`, `views/fragments/AppointmentsFragment.java`, `views/MainActivity.java`, `res/values/strings.xml`.
**Tareas:**
1. Agregar acción "Reagendar" en la card (botón/ícono junto a WhatsApp/Email, o long-press).
2. `AppointmentsAdapter.OnRescheduleClickListener` + `setOnRescheduleClickListener(...)`.
3. `AppointmentsFragment`: al click, `MainActivity.openRescheduleAppointment(appointment)` armando los args.
4. Al volver `rescheduleResult` SUCCESS → `viewModel.loadAppointments(weekDate)` + toast (la lista refleja el turno movido).
**Aceptación:** la acción abre el modo reagendar con el turno correcto; al volver, el turno viejo desaparece y aparece el nuevo.

### T5 — (Opcional / diferible) Trazabilidad viejo↔nuevo
**Bloqueado por:** T1
**Tareas:** migración `ALTER TABLE appointments ADD rescheduled_to_appointment_id INT NULL` (o status `RESCHEDULED`); guardarlo en T1; exponerlo si más adelante se muestra historial.
**Nota:** T1 ya devuelve ambos ids, así que esto es solo auditoría persistente.

### T6 — Documentación y tests
**Bloqueado por:** T1 (tests) / T4 (QA)
**Tareas:**
1. Tests de integración (ver `TESTING.md`, BD `donation_system_test`) para `action=reschedule`: éxito; cancel-first mismo día; 404 donante/turno ajeno; 400 payload; 409.
2. Actualizar `wiki-design-specification.md` (acción en Agenda + modo reagendar) y `technical_summary.md`.
3. `AGENTS.md`/`data-dictionary.md` solo si se agrega columna (T5).

## Verificación
1. `php -l` de los PHP tocados.
2. `scripts\sync-db.ps1` (si hay migración de T5).
3. `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
4. `powershell -ExecutionPolicy Bypass -File .\scripts\check-encoding.ps1 -Quiet` → OK.
5. QA manual: reagendar a fecha posterior; reagendar mismo día a otra hora; reagendar antes de elegibilidad (warning); listado refrescado; el turno viejo no recibe recordatorio.

## Fuera de alcance
- Reagendar desde la ficha del donante o desde el editor de restricciones (queda como está, secundario).
- Notificación automática al donante del cambio de turno (se puede usar el recordatorio manual existente).
- Franjas horarias / límites de hora (el turno ya es de hora libre).

## Riesgos
- **Mismo día**: si no se respeta cancel-first, el chequeo de 2º turno rechaza. Mitigación: transacción con UPDATE antes del INSERT (ya probado en `handleReschedule`).
- **Recordatorios**: la fila nueva arranca con flags en 0; el turno viejo `CANCELLED` no se envía (`send_reminders.php:194`).
- **Código duplicado**: no duplicar la lógica cancel+create; extraer a `appointments_lib.php` (T1).
