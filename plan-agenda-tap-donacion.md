# Plan — Tap en card de agenda → carga de donación

**Fecha:** 2026-10-12
**Estado:** ✅ implementado (2026-10-12). Queda pendiente (separado) el QA visual de cards.

## Objetivo
Al hacer tap en la card de un turno en la pantalla **Agenda** (`AppointmentsFragment`), abrir la pantalla **"cargar una donación"** (`DonationsFragment` / `openClinicalForm`) con:
- los datos del donante de ese turno precargados (contexto: nombre, DNI, grupo, estado),
- el **tipo de intervención del turno** (Sangre/Plasma/Plaquetas) pre-seleccionado en el formulario.

Flujo a replicar (patrón ya existente en `SearchFragment`):
`tap card → cargar Donor → sharedDonorViewModel.setSelectedDonor(donor) → MainActivity.openClinicalForm()`.

## Decisiones resueltas
- **Lookup del donante:** agregar `?id=` a `donors.php` (no tocar payload de `appointments.php`).
- **Zona de tap:** toda la card (botones WhatsApp/email conservan sus listeners).
- **Pre-selección:** preseleccionar el procedimiento del turno en el formulario.

## Contexto técnico relevante
- `Appointment` trae `donorId` (int) pero **no DNI**; `donors.php` hoy solo permite `?dni=`.
- `openClinicalForm()` (MainActivity:81) hace `popBackStack(INCLUSIVE)` → deja el **alta de donante** (`DonorFormFragment.newRegisterInstance()`) como raíz del tab → push `DonationsFragment`. El back desde el formulario vuelve al alta vacía (igual que en búsqueda).
- `DonationsFragment` necesita el `Donor` completo (peso, altura, `nextEligibleDate`, `annualCounts`) → la respuesta de `donors.php?id=` debe tener **la misma forma** que `?dni=`.
- `AppointmentsFragment` ya usa `pendingReminderAppointmentId`/`pendingReminderChannel` solo para recordatorios (no interfiere con el tap).

## Pasos

### 1. Backend: `backend/donors.php`
- Leer también `$id = (int)($_GET['id'] ?? 0)`.
- Si `id > 0`: `SELECT ... WHERE id = :id LIMIT 1` (mismo SELECT de campos que el actual).
- Si no hay `id`: rama actual por `?dni=`. Si ambos vacíos → 400 (`"DNI or ID parameter is required"`).
- Resto del GET (phones, emails, `activeRestrictions`, status, `annualCounts`) **reutilizado tal cual** (ya opera con `$donorId`).
- Backward compatible: `?dni=` responde igual que hoy.

### 2. App — capa de datos
- `api/ApiService.java`: `@GET("donors.php") Call<ApiResponse<Donor>> getDonorById(@Query("id") int id);`
- `repositories/DonorRepository.java`: `getDonorById(int id, Callback<ApiResponse<Donor>> callback)` → `enqueue`.
- `viewmodels/DonorsViewModel.java`: `getDonorById(int)` espejo de `getDonorByDni` (postea a la misma `donorResult` LiveData).

### 3. App — card de agenda
- `adapters/AppointmentsAdapter.java`:
  - Nueva interfaz `OnDonorClickListener { void onDonorClick(Appointment appointment); }`
  - Campo + `setOnDonorClickListener(OnDonorClickListener)`.
  - En `bind()`: `itemView.setOnClickListener(v -> { if (donorClickListener != null) donorClickListener.onDonorClick(appointment); });`
- `res/layout/item_appointment.xml`:
  - `chipAppointmentTime` → `android:clickable="false"` y `android:focusable="false"` (el chip "comería" el tap de la card).
  - Botones `buttonSendWhatsApp`/`buttonSendEmail` intactos (siguen priorizando el recordatorio).

### 4. App — `AppointmentsFragment`
- Agregar `DonorsViewModel donorViewModel` (scoped al fragment) y `SharedDonorViewModel sharedDonorViewModel` (scoped a activity).
- En `setupRecyclers()`: `appointmentsAdapter.setOnDonorClickListener(this::onAppointmentDonorClick);`
- Nuevo campo `private String pendingAppointmentProcedure;`
- `onAppointmentDonorClick(Appointment appointment)`:
  - `pendingAppointmentProcedure = appointment.getTargetProcedure();`
  - `donorViewModel.getDonorById(appointment.getDonorId());`
- Observar `donorViewModel.getDonorResult()`:
  - SUCCESS && data != null → `sharedDonorViewModel.setSelectedDonor(data)`; `sharedDonorViewModel.setSelectedProcedure(pendingAppointmentProcedure)`; `((MainActivity) getActivity()).openClinicalForm();`
  - ERROR → `Toast` con `resource.message` (sin fallback), mismo patrón que `SearchFragment.onDonorResult` (que usa `msg_search_no_results`).

### 5. App — pre-selección del procedimiento
- `viewmodels/SharedDonorViewModel.java`:
  - `MutableLiveData<String> selectedProcedure`
  - `getSelectedProcedure()`, `setSelectedProcedure(String)`, `consumeSelectedProcedure()` (devuelve el valor y lo limpia a null).
- `views/fragments/DonationsFragment.java` (en `observeSharedDonor`, cuando `donor != null`):
  - `String procedure = sharedDonorViewModel.consumeSelectedProcedure();`
  - Si no es null: mapear `PLASMA → buttonTypePlasma`, `PLATELETS → buttonTypePlatelets`, `BLOOD → buttonTypeBlood`.
  - Aplicar con `mutexUpdating = true; toggleGroupDonationType.clearChecked(); mutexUpdating = false;` y luego `toggleGroupDonationType.check(idTarget);` (porque el toggle permite plasma+plaquetas juntos y la lógica de exclusión manual no alcanza para dejar solo uno).
  - Nota: si `clearChecked()` no existiera en la versión de Material Components del proyecto, alternativa: iterar los botones del grupo y `setChecked(false)` cada uno salvo el objetivo.

## Comportamiento final
- Tap en el cuerpo de la card del turno → carga de donación con donante del turno precargado y tipo del turno seleccionado.
- Tap en iconos WhatsApp/email: sigue enviando el recordatorio (sin mezclarse).
- Back desde el formulario → vuelve al **alta de donante vacía** (raíz de la pestaña Donaciones; no devuelve a Agenda).
- `DonationsFragment` restante intacto (ya reacciona al `SharedDonorViewModel`).

## Fuera de alcance
- `AppointmentsAdapter`/`item_appointment` unificados con la card de donante (conclusión previa: no fusionar).
- Cambios en payload de `appointments.php` o modelo `Appointment`.
- QA visual del chip de sangre / meta línea (pendiente de cierre del cambio anterior).

## Verificación
1. `php -l backend/donors.php`.
2. `curl "http://192.168.0.202/donation-system-api/donors.php?id=2"` → misma forma que `?dni=29087311`.
3. `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
4. QA manual en agenda:
   - Tap en card de un turno → abre carga de donación con el donante + tipo pre-seleccionados.
   - Tap en iconos WhatsApp/email → siguen mandando recordatorio.
   - Donante inexistente/error de red → toast sin navegar.
   - After guardar donación: formulario se limpia (vuelve a default Plasma), sin re-aplicar la pre-selección.