# Plan — Historial de turnos (pantalla aparte)

**Fecha:** 2026-10-22
**Estado:** implementado (T1–T6 completos; `assembleDebug` OK; encoding OK; `php -l` limpio).
**Alcance:** separar el histórico de turnos de la Agenda en una pantalla propia (`AppointmentHistoryActivity`), con rango de fechas, filtro por tipo de turno resuelto y orden descendente. La Agenda queda 100% dedicada a turnos activos + agendamiento. Se retira el modo histórico embebido que hoy vive en `AppointmentsFragment` (`VIEW_HISTORY`).

> Reemplaza el enfoque de "histórico embebido con toggle" definido en `plan-agenda-robusta.md` (D13/D16). Sigue siendo la única vista de histórico del proyecto.

## Objetivo
Que el usuario consulte los turnos ya resueltos sin recorrer día por día: una sola pantalla, un rango elegible y un filtro por tipo (Atendido / No vino / Cancelado / Reagendado). Y que la Agenda deje de cargar dos responsabilidades (agenda + histórico) en una misma pantalla.

## Decisiones resueltas
- **H1 — Ubicación:** pantalla aparte (`AppointmentHistoryActivity`), no un modo embebido en la Agenda.
- **H2 — Acceso:** se **reutiliza** el botón existente del header de la Agenda (`buttonToggleHistory`). Pasa a decir siempre **"Ver historial"** y **abre** la pantalla nueva (navegación, no toggle). El botón se llama ahora `buttonOpenHistory` (renombre de id).
- **H3 — Contenido:** el histórico muestra **solo turnos resueltos** (`ATTENDED`, `NO_SHOW`, `CANCELLED`, incluidos los reagendados). Nunca `SCHEDULED`.
- **H4 — Rango:** rango libre con dos `DatePickerDialog` (desde / hasta). Default al entrar: **último mes** (`hoy-1mes` → `hoy`).
- **H4.1 — Sin calendario:** el histórico **no** incluye vista de calendario ni selector por día. El rango se elige solo con los dos `DatePickerDialog`. Un calendario con conteo de resueltos queda **fuera de alcance** (posible ticket futuro, ver "Fuera de alcance").
- **H5 — Alcance del rango:** aplica **solo al histórico**. La Agenda activa sigue por semana.
- **H6 — Orden:** **más reciente primero** (`appointment_date DESC, appointment_time DESC`).
- **H7 — Filtro por tipo:** chips de selección única: `Todos / Atendido / No vino / Cancelado / Reagendado`. Se resuelve en el **cliente** (los datos ya traen `status`, `cancel_reason`, `rescheduledToAppointmentId`).
- **H8 — Etiquetado (derivado, ya definido en D15):** `CANCELLED`+con reemplazo (`rescheduledToAppointmentId > 0`) → **"Reagendado"**; otro `CANCELLED` → **"Cancelado"**; `ATTENDED` → **"Atendido"**; `NO_SHOW` → **"No vino"**.
  - **H8.1 — Filtro por tipo y su mapeo:**
    - `Atendido` → `status = ATTENDED`.
    - `No vino` → `status = NO_SHOW`.
    - `Cancelado` → `status = CANCELLED` **sin** `rescheduledToAppointmentId`.
    - `Reagendado` → `status = CANCELLED` **con** `rescheduledToAppointmentId > 0`.
- **H9 — Chip de estado:** visible siempre en esta pantalla (aquí todos son resueltos).
- **H10 — Volver:** flecha atrás en la toolbar (y back del sistema). **No** hay botón "Ver activos".
- **H11 — Sin acciones de agenda:** en el histórico no se muestran recordatorios, ⋮ de acciones ni botón de agendar. La card es **solo de consulta** (sin tap): no navega al donante, porque el flujo clínico vive dentro de `MainActivity` y esta pantalla es una Activity aparte.
- **H12 — Backend:** se agregan al GET de `appointments.php` los params `from`/`to` (`YYYY-MM-DD`) y el valor de `status` `resolved` (mapea a `a.status <> 'SCHEDULED'`), con orden DESC cuando es histórico.

## Backend (`backend/appointments.php`, rama GET)

Cambios acotados a las líneas 65–113:
1. **Nuevos params:**
   - `from = trim($_GET['from'] ?? '')` y `to = trim($_GET['to'] ?? '')`, validados con `preg_match('/^\d{4}-\d{2}-\d{2}$/', ...)`.
2. **Nuevo valor de status `resolved`:**
   - Antes del bloque de `$statusWhere`: si `$statusFilter === 'resolved'` → `$statusWhere = " AND a.status <> 'SCHEDULED'"` (sin params).
   - El caso `all` permanece como está (compatibilidad).
3. **Rango:** si `from` y `to` son válidos → `WHERE a.appointment_date BETWEEN :from AND :to` (y no entra al branch de `month`/`weekDate`). Si solo hay `from` → `>= :from`; si solo `to` → `<= :to`.
4. **Orden:** cuando hay rango o `status=resolved` → `ORDER BY a.appointment_date DESC, a.appointment_time DESC`. En el resto, mantener ASC.
5. `centerId` se sigue aplicando igual.

Sin cambios en POST ni en el modelo de datos. Sin migración SQL.

## API / datos (app)

- `ApiService`: nueva firma
  ```java
  @GET("appointments.php")
  Call<ApiResponse<List<Appointment>>> getAppointmentsInRange(@Query("from") String from,
                                                             @Query("to") String to,
                                                             @Query("status") String status);
  ```
- `AppointmentRepository`: `getAppointmentsInRange(from, to, status, callback)`.
- `AppointmentsViewModel`: `loadHistory(from, to, status)` + `LiveData<Resource<List<Appointment>>> historyResult`. (`loadMonthAppointments*` y `getMonthResult` se mantienen: los usa el diálogo de calendario de la Agenda.)
- **Nuevo** `HistoryViewModel` **o** reutilizar `AppointmentsViewModel` (se prefiere reusar el existente para no duplicar; el histórico solo usa `loadHistory`).
- `Appointment` ya tiene `getStatus()`, `getCancelReason()`, `getRescheduledToAppointmentId()`: no requiere cambios de modelo.

## App — pantalla nueva

**Nuevos archivos:**
- `views/AppointmentHistoryActivity.java` — `setContentView`, toolbar con back, contiene `AppointmentHistoryFragment` (o todo inline; se prefiere fragment para reusar el patrón `DashboardActivity`).
- `res/layout/activity_appointment_history.xml` — `MaterialToolbar` + `FragmentContainerView`.
- `res/layout/fragment_appointment_history.xml`:
  - Toolbar (título "Historial de turnos", navegación atrás).
  - **Fila de rango:** botón "Desde: dd/MM/yyyy" y "Hasta: dd/MM/yyyy" (dos `MaterialButton` que abren `DatePickerDialog`), + botón "Aplicar" (o recarga al elegir cada fecha).
  - **Chips de tipo:** `ChipGroup` singleSelection (`Todos/Atendido/No vino/Cancelado/Reagendado`).
  - `RecyclerView` (`recyclerViewHistoryList`) reutilizando `item_appointment.xml` + `AppointmentsAdapter`.
  - `TextView` de estado vacío ("No hay turnos en el rango seleccionado").
- `views/fragments/AppointmentHistoryFragment.java` — lógica: estado de rango (default último mes), observa `historyResult`, aplica filtro por chip en cliente, submit al adapter, tap en card → donante.
- Strings nuevas en `values/strings.xml`:
  - `history_title` = "Historial de turnos"
  - `history_range_from` = "Desde: %1$s" / `history_range_to` = "Hasta: %1$s"
  - `history_filter_all` = "Todos", `history_filter_attended` = "Atendido", `history_filter_no_show` = "No vino", `history_filter_cancelled` = "Cancelado", `history_filter_rescheduled` = "Reagendado" (o reusar los `chip_status_*` para las etiquetas visibles de filtro)
  - `history_empty_range` = "No hay turnos en el rango seleccionado"
  - `cd_history_back` = "Volver a la agenda"
- `AndroidManifest.xml`: registrar `.views.AppointmentHistoryActivity` (`label` = `@string/history_title`).

## App — limpieza de la Agenda (`AppointmentsFragment.java`)

Retirar el modo histórico embebido:
- Borrar `VIEW_HISTORY` y toda rama que lo use (`applyViewUi`, `setViewMode`, `emptyStateText`, `currentStatusFilter`).
- Borrar `calendarDayPicked`, `displaySource`, `onCalendarDayPicked`, `matchesViewModeStatus`, `monthAppointments` como fuente y el `date_label_format`/`textViewSelectedDateLabel` si queda sin uso.
- Borrar `onResume` reset de histórico (queda solo recargar la semana).
- `buttonToggleHistory` → renombrar id a `buttonOpenHistory`; en su listener: `startActivity(new Intent(requireContext(), AppointmentHistoryActivity.class))`.
- `buttonToggleRequests` sigue igual (modo solicitudes se queda en la Agenda).
- `currentStatusFilter()` colapsa a `"SCHEDULED"` fijo.
- `applyDayFilter` se simplifica: filtra por fecha + (si `VIEW_REQUESTS`) `hasRescheduleRequest`.
- El Snackbar de undo-histórico se elimina; el de cancelar/editar se mantiene.

> **No tocar:** el botón 📅 (`buttonOpenCalendar`) ni `AppointmentCalendarDialogFragment`/`MonthCalendarAdapter`. El diálogo de calendario de la Agenda queda **intacto** en este plan; su reemplazo por la tira deslizable es un plan aparte (`plan-agenda-dias-deslizables.md`).

**Layout `fragment_appointments.xml`:**
- `buttonToggleHistory` → `buttonOpenHistory`, `android:text="@string/action_view_all"` (que hoy es "Ver historial"). Ya no es toggle.

**Strings a eliminar (tras confirmar que no se usan en otro lado):**
- `btn_view_history`, `week_header_history`, `empty_active_appointments`, `action_view_history` (Snackbar), `date_label_format` (si no se reusa).
- `action_view_all` queda con valor "Ver historial".
- `empty_day_appointments` se conserva (estado vacío de la Agenda activa).

## Flujo de usuario objetivo
1. En la Agenda, toca **"Ver historial"** → se abre `AppointmentHistoryActivity`.
2. Por defecto ve los turnos resueltos del **último mes**, los más recientes primero, con su chip de estado.
3. Toca **"Desde"** / **"Hasta"** → dos `DatePickerDialog` → la lista se recarga para el rango.
4. Toca un chip de tipo (**Atendido / No vino / Cancelado / Reagendado**) → la lista filtra ese subconjunto (en cliente).
5. Toca una card → ve el perfil del donante (igual que en la Agenda).
6. Vuelve con la **flecha atrás**.

## Verificación
- `& .\gradlew.bat assembleDebug` OK.
- `powershell -ExecutionPolicy Bypass -File .\scripts\check-encoding.ps1 -Quiet` → sin hallazgos.
- `& "G:\apps\xampp\php\php.exe" -l backend\appointments.php` limpio.
- Prueba manual: rango default último mes; cambio de rango; cada chip de tipo; orden DESC; back a la Agenda.
- (Opcional) Test de integración backend para `status=resolved` + `from/to` en `AppointmentsLifecycleIntegrationTest` (subir de 68 a N casos verdes). Requiere XAMPP + `donation_system_test`.

## Fuera de alcance
- Selector por calendario (vista de día con conteo de resueltos) dentro del histórico.
- Paginación / lazy loading del histórico.
- Exportar histórico (CSV/PDF).
- Enforcement de roles en backend.
- Cambios al modo `Solicitudes` de la Agenda.
- Reemplazo del diálogo de calendario de la Agenda (ver `plan-agenda-dias-deslizables.md`).

## Orden de implementación (tickets)
- **T1 — Backend:** `from`/`to` + `status=resolved` + orden DESC en `appointments.php`. Verificar con `curl`/navegador.
- **T2 — Datos app:** `ApiService.getAppointmentsInRange`, `AppointmentRepository.getAppointmentsInRange`, `AppointmentsViewModel.loadHistory` + `historyResult`.
- **T3 — Pantalla:** `AppointmentHistoryActivity` + layouts + manifest + strings.
- **T4 — UI histórico:** rango (dos DatePickers, default último mes) + chips de tipo + lista + tap a donante + vacío.
- **T5 — Limpieza Agenda:** retirar `VIEW_HISTORY` y asociados; renombrar botón; conectar navegación.
- **T6 — Verificación y docs:** build + encoding + php -l; actualizar `data-dictionary.md` (si aplica) y `TESTING.md` (QA del histórico).
