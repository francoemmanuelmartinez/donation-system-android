# Plan — Superadmin + perfil de usuario + "atendido por" + multi-centro

**Fecha:** 2026-10-19
**Estado:** plan aprobado, pendiente de implementación (feature grande, multi-fase).
**Alcance:** solo este cambio; no afecta el flujo actual de donaciones/Agenda.

## Objetivo

1. Agregar el rol **SUPERADMIN** por encima de DIRECTORA, que gestiona usuarios y centros.
2. Eliminar el rol **NONE** (registro queda pendiente con rol por defecto `ADMINISTRATIVA` sin aprobar).
3. Crear una **pantalla de perfil de usuario** (nombre, apellido, email, teléfono, rol) editable por el propio usuario.
4. Registrar **"atendido por"** en cada donación (ya capturado vía `staff_user_id`, pero falta exponerlo en listados/histórico).
5. Soportar **múltiples centros de donación**, decidiendo entre multi-tenant (BD por centro) vs. single-DB con `center_id`.

## Decisiones resueltas

- Roles finales: `ADMINISTRATIVA`, `DIRECTORA`, `SUPERADMIN` (se quita `NONE`).
- Superadmin está por encima de Directora; gestiona centros y usuarios globales.
- "Atendido por" = perfil de usuario con nombre/apellido/mail/teléfono/rol.
- Multi-centro: definir arquitectura (ver trade-off §3).

## Contexto técnico relevante

- Hoy no hay concepto de centro en ningún lado (`sql/`, `backend/`, `app/`).
- `users.role` es `ENUM('NONE','ADMINISTRATIVA','DIRECTORA')` (`sql/donation_system.sql:31`); mutex de último director en `users_admin.php:78-130`.
- `donations.staff_user_id` ya existe (FK `users.id`) y se captura desde `SessionManager.getUserIdOrNull()` (`DonationsFragment.java:535`).
- Estadísticas (`statistics.php`) agregan todo el sistema sin filtro.
- `RoleMapper` y `LoginFragment.routeByRole` solo conocen ADMINISTRATIVA/DIRECTORA.
- No hay histórico/listado de donaciones en la app (solo alta), así que "atendido por" hoy solo se guarda, no se consulta.

## Trade-off: multi-tenant vs. single-DB con `center_id`

| Enfoque | Pros | Contras |
|---|---|---|
| **A — Single DB + `center_id`** | Un solo deploy, un solo esquema, stats por centro con `WHERE center_id = ?`, más simple de operar | Requiere agregar `center_id` a `users`, `donors`, `donations`, `appointments`, `donor_restrictions`; todas las queries deben filtrar por centro; riesgo de cross-center leak si se olvida un filtro |
| **B — BD por centro (multi-tenant)** | Aislamiento total de datos, backup/restore por centro, escalable | Múltiples conexiones PHP (config por centro), migraciones N veces, complejidad de scheduler/lazy sweep por centro, app debe saber a qué centro conectarse |

**Recomendación inicial:** Enfoque A (single DB + `center_id`) para una primera versión; si crece, migrar a B. Investigar costo de migración antes de decidir.

## Decision tomada: Enfoque A (single DB + `center_id`) + reglas de acceso

Cada centro tiene sus propios donantes, staff y datos, pero todo vive en una sola BD. El aislamiento se logra por filtro, no por infraestructura.

### Reglas de aislamiento por centro

- `users.center_id` = centro al que pertenece el usuario.
- `donors.center_id`, `donations.center_id`, `appointments.center_id`, `donor_restrictions.center_id` = centro donde se registró.
- **Staff regular** (ADMINISTRATIVA/DIRECTORA): todas las queries filtran `WHERE center_id = su center_id`. No ve datos de otros centros.
- **SUPERADMIN**: sin filtro por defecto (ve todo) o puede seleccionar un centro para filtrar. Tiene ABM de centros.
- La app envía `center_id` del usuario logueado en cada request; el backend aplica el filtro según el rol.
- **Mitigación de cross-center leak:** revisar todas las queries en `backend/*.php` y agregar tests de aislamiento (Fase 5).

## Pasos (fases)

### Fase 0 — Diseño de datos (sin código)
1. Definir tabla `centers` (id, nombre, dirección, activo).
2. Agregar `center_id` (FK a centers) a `users`, `donors`, `donations`, `appointments`, `donor_restrictions`.
3. Decidir si `restrictions` (diccionario) y `donation_recovery_policies` son globales o por centro.
4. Definir si un usuario puede pertenecer a varios centros (N:M) o uno solo.

### Fase 1 — Roles y usuarios (backend)
5. Actualizar ENUM de `users.role` → `('ADMINISTRATIVA','DIRECTORA','SUPERADMIN')`.
6. Eliminar `NONE`: registro crea usuario con rol `ADMINISTRATIVA` + `is_approved=0` (pendiente de aprobación por Directora/Superadmin).
7. `users_admin.php`: aceptar `SUPERADMIN`; reglas de mutex centralizadas (mínimo 1 superadmin activo, mínimo 1 directora por centro).
8. `login.php`: propagar `centerId` (y datos de perfil) en respuesta.
9. Endpoints ABM de centros (solo superadmin).

### Fase 2 — Perfil de usuario (backend + app)
10. Endpoint `GET/PUT profile` (nombre, apellido, email, teléfono, rol read-only).
11. App: `ProfileFragment`/Activity, repositorio, VM, navegación (botón "Mi perfil" en drawer o dashboard).
12. `SessionManager`: persistir datos de perfil (para mostrar "atendido por" sin fetch).

### Fase 3 — "Atendido por" (backend + app)
13. Ya se guarda `staff_user_id` en `donations` (y `created_by_user_id` en `donor_restrictions`). Falta exponerlo.
14. Agregar JOIN a `users` en respuestas de donaciones (si se agrega endpoint de histórico) o en estadísticas.
15. App: mostrar "Atendido por: {nombre}" en el formulario clínico y en estadísticas por staff.

### Fase 4 — Multi-centro (backend + app)
16. Migrar esquema (agregar `center_id` everywhere, backfill para datos existentes).
17. Filtrar todas las queries por `center_id` (donors, donations, appointments, restrictions, statistics).
18. App: selector de centro en login o en perfil; persistir en sesión; enviar `center_id` en todas las requests.
19. Superadmin: vista global (todos los centros) con filtro por centro en estadísticas y usuarios.

### Fase 5 — Tests y documentación
20. Ampliar suite de integración: roles nuevos, perfil, centros, filtros por centro.
21. Actualizar `wiki-design-specification.md` (nueva card en dashboard, perfil, centros), `technical_summary.md` (roles, multi-centro), `AGENTS.md` (si cambia el esquema).

## Fuera de alcance
- Migración de datos de centros existentes (se asume centro único inicial).
- App de escritorio C++ (roadmap separado).
- Cambios al flujo de donación/Agenda (no se tocan).

## Verificación
- `./gradlew assembleDebug` + `testDebugUnitTest` en verde.
- Checklist manual: login por rol, perfil editable, "atendido por" visible, estadísticas filtradas por centro, mutex de superadmin.
- `check-encoding.ps1`.

## Riesgos
- **Cross-center data leak** si se olvida un filtro `center_id` en alguna query. Mitigación: revisar todas las queries en `backend/*.php` y agregar tests de aislamiento.
- **Migración de esquema** con datos existentes: requiere backfill (`center_id = 1` para todo lo existente).
- **Scheduler/lazy sweep** por centro: si se va a multi-tenant (enfoque B), el dispatcher debe iterar centros. Con enfoque A, un solo dispatcher alcanza.

## Pendiente: ampliación de la suite de integración

La suite actual (58 tests) no cubre los cambios de Fases 1-4. Falta agregar:

### Roles y usuarios
- Login con rol SUPERADMIN → enruta a DirectorDashboardActivity.
- Registro crea usuario con rol ADMINISTRATIVA (no NONE) + is_approved=0.
- approve con rol SUPERADMIN (solo superadmin puede asignarlo).
- update_role degradando al último DIRECTORA → 403.
- update_role degradando al último SUPERADMIN → 403.
- delete del último DIRECTORA → 403.
- delete del último SUPERADMIN → 403.

### Perfil
- GET profile.php?userId=X → devuelve perfil completo.
- PUT profile.php → actualiza nombre, apellido, email, teléfono.
- PUT con email duplicado → 409.
- PUT sin campos obligatorios → 400.

### Centros (ABM)
- centers action=list (solo superadmin) → devuelve lista.
- centers action=create (solo superadmin) → crea centro.
- centers action=create sin nombre → 400.
- centers action=update → actualiza centro.
- centers action=* con requesterRole != SUPERADMIN → 403.

### Multi-centro (aislamiento)
- Donor creado en centro A no aparece en búsqueda del centro B.
- Donación registrada en centro A no aparece en estadísticas del centro B.
- Turnos de centro A no aparecen en agenda del centro B.
- Superadmin (centerId=0 o no enviado) ve datos de todos los centros.
- Staff de centro A no puede ver datos de centro B (filtro aplicado).

### Atendido por
- POST donations con staffUserId → se guarda correctamente.
- GET donations?staffUserId=X → devuelve solo donaciones de ese staff.
- statistics?staffUserId=X → byStaff y totales filtrados.
- statistics?centerId=X → totales filtrados por centro.

### Nota
Los tests de integración requieren XAMPP levantado y la BD `donation_system_test`. El fixture SQL (`donation_system_test.sql`) ya incluye la tabla `centers` y `center_id` en todas las tablas.
- Hoy no hay histórico de donaciones (solo alta), por lo que "atendido por" se usa únicamente al registrar; para reportes por staff habría que agregar consulta/listado.
- El mutex de "último director" está duplicado en `update_role` y `delete`; con superadmin conviene centralizar reglas (mínimo un superadmin, etc.).
- No hay tests de roles más allá de integración de login/usuarios; conviene ampliar.
