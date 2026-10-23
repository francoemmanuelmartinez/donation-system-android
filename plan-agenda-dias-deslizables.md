# Plan — Agenda con días deslizables (reemplazo del calendario modal)

**Fecha:** 2026-10-22
**Estado:** implementado (2026-10-23). T1–T6 completos.
**Dependencia:** se implementó **después** de `plan-historial-turnos.md`.

**Alcance:** reemplazar la tira fija de 5 días (Lun–Vie) + el diálogo de calendario mensual por una **tira horizontal deslizable de días con conteo**, para navegar los turnos activos futuros en la misma pantalla, sin modal. Se retira el diálogo de calendario de la Agenda.

## Contexto y motivo
- Hoy conviven dos mecanismos para elegir día: la tira fija de 5 días (`recyclerViewDaysFilter`) y el diálogo modal `AppointmentCalendarDialogFragment` (que trae todos los estados y del que la lista luego filtra). Dos paradigmas para lo mismo.
- El modal implica un salto de contexto (abrir → navegar mes → tocar día → cerrar) y mezcla responsabilidades (traer todos los estados).
- Con **40-60 turnos/mes (~2-3 por día hábil)** y búsqueda frecuente de **días lejanos**, una tira deslizable con conteo cubre el caso: se ve el panorama al scrollear y se salta a cualquier día con un tap, sin modal.

## Decisiones resueltas
- **S1 — Paradigma único:** la navegación de día de la Agenda activa es una **tira horizontal continua de días** (`Horizontal RecyclerView`), no una tira fija de 5 + un modal.
- **S2 — Conteo por día:** cada celda muestra el **número de turnos activos** (`SCHEDULED`) de ese día. El conteo sale del mismo fetch de la Agenda (no un endpoint nuevo).
- **S3 — Rango de la tira:** arranca en **hoy** y se extiende hacia adelante (p. ej. ~90 días / 3 meses visibles por scroll). Sin días pasados en la Agenda activa (lo pasado es histórico, ver `plan-historial-turnos.md`).
- **S4 — Selección:** un tap en un día selecciona ese día y lista sus turnos activos. Es una selección **inline** (sin modal).
- **S5 — Botón "Hoy":** atajo para volver a hoy (reusar el espacio de header existente o un botón en la fila de la tira).
- **S6 — Retiro del modal:** se elimina `AppointmentCalendarDialogFragment`, `MonthCalendarAdapter`, `item_month_day.xml`, `dialog_appointment_calendar.xml`, el botón 📅 (`buttonOpenCalendar`) del header y la API de mes del ViewModel (`loadMonthAppointments`/`getMonthResult`) **si no queda otro consumidor** (verificar antes de borrar).
- **S7 — Orden de la tira:** ascendente por fecha (hoy → futuro). El día seleccionado se mantiene visible (scroll a la posición).
- **S8 — Estado vacío:** si el día seleccionado no tiene activos → "No hay turnos activos para este día" (texto existente o nuevo).

## Alcance funcional
- La Agenda activa queda: **tira de días deslizable con conteo** + **lista del día seleccionado** + botones de header (`Ver historial` → navega; `Solicitudes`; `✉` dispatch). El 📅 desaparece.
- Sin cambios de backend: el conteo se deriva del fetch de turnos activos que ya trae la Agenda.

## App — cambios
**Layout `fragment_appointments.xml`:**
- `recyclerViewDaysFilter` se mantiene como `Horizontal RecyclerView` (ahora deslizable sobre muchos días).
- Quitar `buttonOpenCalendar` del header (y el `Space` que lo separaba si queda sin sentido).
- Ajustar `textViewSelectedDateLabel` (puede quedar o eliminarse según cómo se muestre el día seleccionado en la tira).

**Adapter de días (nuevo/adaptado):**
- Nuevo `DayStripAdapter` (o adaptar `DayFilterAdapter`): celda con **día de semana + número + conteo**. `LocalDate`, `count`, `selected`.
- Item layout `item_day_strip.xml` (reemplaza o extiende `item_day_filter.xml`).

**`AppointmentsFragment.java`:**
- Construir la lista de días desde hoy (S3) en vez de los 5 días Lun–Vie.
- Cargar los conteos por día a partir de los turnos activos traídos por el fetch (o un fetch de rango para el conteo del mes — decidir en implementación; preferido: mismo fetch de la semana ampliada).
- `openCalendar()` y el listener del 📅 se eliminan.
- `applyDayFilter` sigue filtrando por fecha (sin ramas de histórico, que ya se retiraron en el plan anterior).
- Scroll a la posición del día seleccionado al seleccionar/volver.

## Flujo de usuario objetivo
1. Abre la Agenda → ve la tira de días desde hoy, cada uno con su conteo de activos.
2. Scrollea la tira para ver los próximos días (panorama) y toca un día → la lista muestra sus turnos activos.
3. Toca **"Hoy"** para volver de un salto al día actual.
4. **"Ver historial"** sigue abriendo la pantalla de histórico (plan aparte).
5. No hay botón de calendario ni modal.

## Verificación
- [x] `& .\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
- [x] `powershell -ExecutionPolicy Bypass -File .\scripts\check-encoding.ps1 -Quiet` → `OK: 276 archivos`.
- [x] `.\gradlew.bat testDebugUnitTest` → suite en verde (con override de BD de test).
- [x] Sin referencias colgando a `AppointmentCalendarDialogFragment`/`MonthCalendarAdapter`/`DayFilterAdapter`/`loadMonthAppointments`/`getMonthResult`/`getAppointmentsByMonth*`.
- [ ] Prueba manual: scroll de la tira; conteo correcto por día; selección de un día lejano; botón "Hoy"; sin botón 📅; "Ver historial" abre la pantalla.

## Resultado de implementación
- **T1:** `item_day_strip.xml` + `adapters/DayStripAdapter.java` (día de semana + número + badge de conteo; seleccionado = fondo `medical_blue` + texto blanco).
- **T2:** `AppointmentsFragment.setupStrip()` arma 90 días desde hoy; `setupRecyclers()` usa `DayStripAdapter`; `selectDay()` selecciona inline y hace `scrollToPosition`; botón **"Hoy"** (`buttonToday`).
- **T3:** `recomputeDayCounts()` deriva el conteo por día del fetch de activos (`allAppointments`), sin endpoint nuevo.
- **T4:** retirados `buttonOpenCalendar` y su listener; borrados `AppointmentCalendarDialogFragment`, `MonthCalendarAdapter`, `item_month_day.xml`, `dialog_appointment_calendar.xml`, `DayFilterAdapter`, `item_day_filter.xml`; removidas del ViewModel/repo/ApiService `loadMonthAppointments`/`getMonthResult`/`getAppointmentsByMonth*` (sin otros consumidores).
- **T5:** header en dos filas (título + "Hoy"; botones + dispatch); eliminado `textViewSelectedDateLabel`; strings huérfanas fuera (`week_header_format`, `cd_open_calendar`, `cd_prev_month`, `cd_next_month`); header usa `date_label_format` = "Turnos del %1$s"; nuevo `action_today`.
- **T6:** build + encoding + suite verificados.

## Fuera de alcance
- Días pasados en la tira (los cubre el histórico).
- Capacidad por franja / feriados / recursos.
- Vista de mes completa dentro de la Agenda.

## Orden de implementación (tickets)
- **T1 — Tira:** `item_day_strip.xml` + `DayStripAdapter` (día + número + conteo + seleccionado).
- **T2 — Fragment:** construir los días desde hoy, selección inline, scroll al seleccionado, botón "Hoy".
- **T3 — Conteo:** derivar el conteo por día de los turnos activos (fetch de semana ampliada o rango).
- **T4 — Retiro del modal:** quitar `buttonOpenCalendar` y su listener; borrar `AppointmentCalendarDialogFragment`, `MonthCalendarAdapter`, `item_month_day.xml`, `dialog_appointment_calendar.xml` y la API de mes del ViewModel (si no tiene otro consumidor).
- **T5 — Layout/strings:** limpiar header y strings huérfanas (`cd_open_calendar`, textos del diálogo).
- **T6 — Verificación:** build + encoding + prueba manual + greps de referencias colgando.
