# QA Manual — Checklist de Pruebas

Checklist para probar la app a mano contra el backend real (XAMPP). Ejecutar en el emulador de Android Studio.

## 0. Preparación

- [ ] XAMPP levantado (Apache + MySQL en la ruta de tu dispositivo — ver `AGENTS.md` →
      "Dispositivos"; puertos 80 y 3306). En la notebook, un 500 `Access denied for user 'root'`
      indica que `MySQL97` tomó el 3306: ver `AGENTS.md` → "Puerto 3306 ocupado por MySQL97".
- [ ] El API responde en `http://localhost/donation-system-api/` (la carpeta es un junction al repo `backend/`).
- [ ] La app en el emulador apunta a `http://10.0.2.2/donation-system-api/` (default de `RetrofitClient`).
- [ ] La app usa la base `donation_system` (datos reales). El modo de test automatizado
      (override hacia `donation_system_test`) está documentado más abajo en
      "Reactivar el test automatizado".

### Acceso seed
- En `donation_system` (datos reales) creá una Directora desde la pantalla de registro o
  insertándola directo en MySQL (hash BCrypt). Las 4 restricciones iniciales del diccionario están
  sembradas en la BD real.
- La cuenta `director@test.local` / `password` existe **solo** en `donation_system_test`.

## 1. Autenticación

- [ ] Login correcto de una Directora aprobada (seed) → entra al Home con su rol.
- [ ] Login con credenciales inválidas → mensaje de error (401).
- [ ] Login de un usuario pendiente de aprobación → mensaje *"Cuenta pendiente de aprobación por la Dirección."* (403).
- [ ] Campos vacíos → validación local (sin llamada de red).
- [ ] Registro de cuenta nueva (con teléfono opcional) → queda NONE / pendiente.
- [ ] DNI o email repetido en registro → 409 con mensaje claro.
- [ ] Registro con campos obligatorios incompletos → 400.

## 2. Gestión de usuarios (solo Directora)

- [ ] Lista de pendientes muestra el usuario recién registrado.
- [ ] Aprobar usuario con rol ADMINISTRATIVA o DIRECTORA → aparece en la lista activa.
- [ ] Aprobar con rol inválido → rechazado con mensaje.
- [ ] Cambiar rol de una Directora cuando hay 2 o más → permitido.
- [ ] Degradar a la única Directora → 403 con *"Operación denegada. No se puede degradar al único usuario con rol de Directora en el sistema."*
      (validar que el mensaje real del backend llegue a la UI).
- [ ] Eliminar a la única Directora → 403 con *"Operación denegada. No se puede eliminar al único usuario con rol de Directora en el sistema."*.
- [ ] Eliminar a un usuario normal → se elimina y refresca la lista.

## 3. Donantes

- [ ] Guardar donante completo (datos + teléfono + email).
- [ ] Editar donante existente por DNI → actualiza el mismo registro (no duplica).
- [ ] Guardar sin campos obligatorios → 400 con mensaje.
- [ ] Buscar donante por DNI → datos + contactos parseados correctamente
      (números/emails visibles, primary marcado).
- [ ] DNI inexistente → mensaje de no encontrado.

## 4. Restricciones (desde la pantalla de Donaciones)

- [ ] El alta de donante **no** muestra Aptitud/Exclusión; el donante se guarda siempre APTO.
- [ ] Checkbox **"Aplicar restricciones (No apto)"**: oculta la carga de donación y cambia el botón a *"GUARDAR RESTRICCIONES"*.
- [ ] Asignar restricción con veda (días) → el donante pasa a EN_VEDA y se calcula la fecha de retorno laborable (salta fines de semana).
- [ ] Asignar **veda permanente** → donante EN_VEDA sin fecha de retorno.
- [ ] Restricciones activas listadas en la card: **EDITAR** (in-place, conserva la fecha de inicio) y **QUITAR** (`revoke` → APTO si no quedan otras; el diálogo de confirmación pregunta «¿Incluir ahora…?»).
- [ ] Con turno en la Agenda, al guardar se ofrece **reagendar**: cancela el turno actual y crea uno nuevo en la fecha de retorno.
- [ ] Crear restricción nueva en el diccionario (panel Directora) → aparece en el dropdown.
- [ ] Crear sin nombre → 400.
- [ ] Eliminar restricción → desaparece del diccionario.

## 5. Búsqueda

- [ ] Búsqueda por texto (DNI, nombre o apellido) → devuelve coincidencias (prefijo).
- [ ] Filtros por grupo/factor → acotan por tipo sanguíneo (se combinan con **AND**: el donante debe coincidir con grupo **y** factor).
- [ ] **Se devuelven todos los donantes, incluidos los que están en veda** (el estado APTO/EN_VEDA se muestra en la tarjeta, no se filtra).
- [ ] Sin coincidencias → lista vacía (SUCCESS, no error).
- [ ] Búsqueda vacía (sin criterios) → devuelve todos los donantes, aptos primero, paginado.
- [ ] Con texto → orden alfabético (apellido, nombre); sin texto → aptos primero.
- [ ] Paginación: `hasMore` y "cargar más" al llegar al final de la lista.
- [ ] Donante con veda activa se muestra con su estado y motivo de veda.

## 6. Donaciones

- [ ] Registrar donación con signos vitales y 1 o más procedimientos.
- [ ] Se guarda `last_donation_date` = día de la donación.
- [ ] Procedimientos inválidos se ignoran (solo PLASMA / PLATELETS / BLOOD).
- [ ] Parámetros clínicos faltantes → 400.
- [ ] Donante con días de recuperación pendientes (`next_eligible_date` futura) → **advertencia no
      bloqueante**: el diálogo solo avisa y permite continuar igual.
- [ ] Donante que alcanza/supera el tope anual de su política → **advertencia no bloqueante**
      (mismo comportamiento: avisa, no impide guardar).

## 7. Turnos y agenda

- [ ] La lista carga turnos con donante y teléfono.
- [ ] Enviar recordatorio (WhatsApp o email) → persiste su bandera independiente +
      usuario + fecha de envío (una no invalida a la otra).
- [ ] `appointmentId` inválido (`<= 0`) → 400. Un id inexistente (`> 0`) no da error: el UPDATE afecta 0 filas y responde success.
- [ ] **Agendar turno nuevo:** botón "AGENDAR UN DONANTE" → buscar por DNI → elegir fecha
      hábil (hoy deshabilitado en el selector de fecha) y hora → procedimiento → confirmar → el turno aparece en la Agenda.
- [ ] **Tap en la card de un turno** (cuerpo, no los iconos) → abre la carga de donación con el
      donante del turno precargado y su procedimiento preseleccionado.
- [ ] **Envío masivo:** botón de dispatch (`dispatch_reminders`, `manual=true`) → procesa
      secuencialmente, respeta la ventana horaria 08:00–13:00 y marca los flags por canal
      (ver `whatsapp-reminders-guide.md` §7).

### 7.1 Ciclo de vida del turno (plan-agenda-robusta)

- [ ] **Menú de acciones (⋮)** aparece solo en turnos `SCHEDULED` (oculto para `ATTENDED` / `NO_SHOW` / `CANCELLED`).
- [ ] **Editar turno** → formulario en modo EDIT (sin búsqueda, con fecha/hora/procedimiento precargados) → guardar aplica los cambios y refresca la Agenda.
- [ ] **Reagendar** → formulario en modo RESCHEDULE → al confirmar, el turno viejo queda `CANCELLED` con `cancel_reason='RESCHEDULED'` y `rescheduled_to_appointment_id` apuntando al nuevo; el nuevo queda `SCHEDULED`.
- [ ] **Cancelar** (motivo opcional, max 120) → turno sale del activo, aparece en histórico con chip "Cancelado"; si fue reagendado, el chip dice "Reagendado".
- [ ] **Atendido / No vino** (solo turnos de fecha pasada `SCHEDULED`) → cambia el estado; `NO_SHOW` disponible únicamente para fechas anteriores a hoy.
- [ ] `ATTENDED` / `NO_SHOW` están protegidos: no se pueden cancelar ni reagendar (sin acciones en el ⋮).
- [ ] **Registrar donación desde un turno** → setea `donations.appointment_id` y marca el turno `ATTENDED` en la misma transacción.
- [ ] **Alerta WhatsApp** al cancelar/reagendar (post-commit, best-effort): se dispara si `REMINDER_ENABLED` está activo y el donante tiene teléfono; el fallo del envío no revierte la operación.
- [ ] **Ver todos / histórico:** botón de texto en el header alterna activo ↔ histórico; el chip de estado solo se muestra en histórico; estado vacío "No quedan turnos activos".
- [ ] **Solicitudes (D18):** un donante que pidió reagendar aparece en modo "Solicitudes" con chip "Pidió reagendar"; al reagendar/editar el turno, la marca se limpia.
- [ ] Fecha pasada rechazada: `create_appointment` y `update_appointment` con fecha `< hoy` → 400.

## 8. Políticas de recuperación (panel Directora)

- [ ] Abrir "Políticas de recuperación" desde el dashboard de la Directora → lista las
      políticas baseline (una por procedimiento: PLASMA / PLATELETS / BLOOD).
- [ ] Editar una política (días de recuperación / tope anual, hombre y mujer) → guarda y
      refresca la lista.
- [ ] Crear política nueva para uno de los 3 procedimientos (PLASMA / PLATELETS / BLOOD) → aparece en la lista y en el dropdown.
- [ ] Eliminar política → desaparece de la lista.
- [ ] Los cambios se reflejan en las advertencias del formulario clínico (§6): al bajar los
      días de recuperación, un donante que estaba en advertencia deja de advertir (y viceversa).

## 9. Estadísticas

- [ ] Rangos day / week / month / year renderizan tarjetas y serie.
- [ ] Con donaciones de hoy → counts correctos por procedimiento.
- [ ] Donaciones de hace meses solo entran en rango `year`.
- [ ] Rango inválido → 400 (no crashea la app).

## 10. Automatizado (opcional, al final)

- [ ] `.\gradlew.bat testDebugUnitTest` → 68/68 en verde (requiere XAMPP levantado).
- [ ] `.\gradlew.bat assembleDebug` → BUILD SUCCESSFUL.

## Reactivar el test automatizado

Para volver a apuntar el API a la base de test (`donation_system_test`, necesaria para
`testDebugUnitTest`), recrear `backend/config_override.db.php`:

```php
<?php
$db_name = "donation_system_test";
?>
```

Advertencias:
- Mientras exista ese archivo, la app del emulador también usa `donation_system_test`.
- `testDebugUnitTest` crea y resetea esa base en cada corrida (drop + recreate del esquema).
- **Nunca** correr la suite sin el override: el helper de test aplica drop+recreate **siempre** sobre `donation_system_test` (nombre fijo), independientemente de la base a la que apunte el API.
- `scripts\sync-db.ps1` **no** toca `donation_system_test` sin `-Force` (guarda contra pisarla por error).