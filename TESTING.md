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

### Acceso seed (solo en `donation_system_test`)
- La cuenta `director@test.local` / `password` y las 4 restricciones iniciales existen únicamente
  en la base de test. En `donation_system` (datos reales) creá una Directora desde la pantalla de
  registro o insertándola directo en MySQL (hash BCrypt).

## 1. Autenticación

- [ ] Login correcto de una Directora aprobada (seed) → entra al Home con su rol.
- [ ] Login con credenciales inválidas → mensaje de error (401).
- [ ] Login de un usuario pendiente de aprobacion → mensaje *"Cuenta pendiente de aprobacion"* (403).
- [ ] Campos vacíos → validación local (sin llamada de red).
- [ ] Registro de cuenta nueva (con teléfono opcional) → queda NONE / pendiente.
- [ ] DNI o email repetido en registro → 409 con mensaje claro.
- [ ] Registro con campos obligatorios incompletos → 400.

## 2. Gestión de usuarios (solo Directora)

- [ ] Lista de pendientes muestra el usuario recién registrado.
- [ ] Aprobar usuario con rol ADMINISTRATIVA o DIRECTORA → aparece en la lista activa.
- [ ] Aprobar con rol inválido → rechazado con mensaje.
- [ ] Cambiar rol de una Directora cuando hay 2 o más → permitido.
- [ ] Degradar a la única Directora → 403 con *"No se puede degradar al único usuario..."*
      (validar que el mensaje real del backend llegue a la UI).
- [ ] Eliminar a la única Directora → 403 con *"No se puede eliminar al único usuario..."*.
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
- [ ] Restricciones activas listadas en la card: **Editar** (in-place, conserva la fecha de inicio) e **Incluir ahora** (`revoke` → APTO si no quedan otras).
- [ ] Con turno en la Agenda, al guardar se ofrece **reagendar**: cancela el turno actual y crea uno nuevo en la fecha de retorno.
- [ ] Crear restricción nueva en el diccionario (panel Directora) → aparece en el dropdown.
- [ ] Crear sin nombre → 400.
- [ ] Eliminar restricción → desaparece del diccionario.

## 5. Búsqueda

- [ ] Búsqueda por texto (DNI, nombre o apellido) → devuelve coincidencias (prefijo).
- [ ] Filtros por grupo/factor → acotan por tipo sanguíneo (OR implícito entre grupo y factor).
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

## 7. Turnos (appointments)

- [ ] La lista carga turnos con donante y teléfono.
- [ ] Enviar recordatorio → persiste bandera + usuario + fecha de envío.
- [ ] Turno inexistente → 400/404.

## 8. Estadísticas

- [ ] Rangos day / week / month / year renderizan tarjetas y serie.
- [ ] Con donaciones de hoy → counts correctos por procedimiento.
- [ ] Donaciones de hace meses solo entran en rango `year`.
- [ ] Rango inválido → 400 (no crashea la app).

## 9. Automatizado (opcional, al final)

- [ ] `.\gradlew.bat testDebugUnitTest` → 58/58 en verde (requiere XAMPP levantado).
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
- **Nunca** correr la suite sin el override: el helper de test aplica drop al esquema en la base sobre
  la que el API esté apuntando en ese momento.
- `scripts\sync-db.ps1` **no** toca `donation_system_test` sin `-Force` (guarda contra pisarla por error).