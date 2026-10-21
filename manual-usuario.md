# Manual de uso — Donation System (banco de plasma/plaquetas)

Narrativa de lo que el usuario puede hacer en la app y qué datos ingresa en cada paso. Complementa `flujo-usuario.md` (diagramas técnicos) y `data-dictionary.md` (tablas/campos).

---

## 1. Acceso al sistema

### 1.1 Inicio de sesión
- **Pantalla:** Login
- **Datos que ingresa:**
  - Correo electrónico o DNI
  - Contraseña
- **Acción:** botón "INICIAR SESIÓN"
- **Resultado:** si las credenciales son válidas y la cuenta está aprobada, ingresa al sistema. Si no, mensaje de error (401/403).
- **Si no tiene cuenta:** botón "¿No tenés cuenta? Registrate acá" → pantalla de registro.

### 1.2 Registro de cuenta nueva
- **Pantalla:** SignUp
- **Datos que ingresa:**
  - Nombre/s
  - Apellido/s
  - DNI
  - Correo electrónico
  - Teléfono (opcional)
  - Contraseña
- **Acción:** botón "REGISTRARSE"
- **Resultado:** la cuenta se crea con rol Administrativa y queda **pendiente de aprobación** por una Directora. Mensaje: "Cuenta registrada con éxito. Esperando aprobación de Dirección."
- **Validaciones:** DNI o email duplicado → 409; campos obligatorios vacíos → 400.

### 1.3 Aprobación de cuenta (solo Directora/Superadmin)
- **Pantalla:** Dashboard → "Usuarios" → tab "Pendientes"
- **Acción:** seleccionar rol (Administrativa/Directora/Superadmin) → botón "APROBAR"
- **Datos que se asignan:** rol, centro (por defecto el del aprobador), aprobador.
- **Resultado:** la cuenta queda activa y puede iniciar sesión.

---

## 2. Alta de donante

- **Pantalla:** Tab "Registrar Donante" (raíz del tab)
- **Modos:** Registro, Consulta (solo lectura), Modificación.

### Sección 1 — Datos personales
- Apellido/s
- Nombre/s
- DNI (numérico)
- Género (dropdown: Masculino/Femenino/Otro)
- Fecha de nacimiento (selector de calendario) → calcula edad automáticamente

### Sección 2 — Contacto y ubicación
- Ciudad
- Dirección
- **Teléfonos** (dinámicos): número + tipo (Mobile por defecto). Botón "Agregar otro teléfono". Filas agregadas tienen botón para eliminar.
- **Emails** (dinámicos): dirección + tipo (Personal por defecto). Botón "Agregar otro email".

### Sección 3 — Datos críticos
- Grupo y factor sanguíneo (dropdown, ej. O+)
- Fenotipo (ej. Kell-)
- Peso (kg, decimal)
- Altura (cm, decimal)
- Fecha de última donación (solo lectura, se completa al registrar donaciones)

### Acción
- Botón "CARGAR DONANTE" (guarda o modifica).
- **Resultado:** donante creado como APTO. Si ya existe por DNI, actualiza el registro (no duplica).
- **Validaciones:** campos obligatorios vacíos → 400.

---

## 3. Formulario de donación (clínico)

- **Pantalla:** Se abre automáticamente después de guardar un donante, o desde Búsqueda/Agenda.
- **Cabecera:** nombre, DNI, grupo sanguíneo, badge de estado (APTO/EN_VEDA), botón "VER DATOS".

### Datos que ingresa
- **Hemograma:**
  - Hematocrito (HTO %)
  - Recuento de plaquetas (u/µL)
- **Tipo de procedimiento** (toggle, selección múltiple con mutex):
  - Plasma
  - Plaquetas
  - Sangre (exclusivo: al seleccionar Sangre se deseleccionan Plasma/Plaquetas)
- **Restricciones (opcional):** checkbox "Aplicar restricciones (No apto)" → oculta la carga de donación y cambia el botón a "GUARDAR RESTRICCIONES":
  - Tipo de restricción (dropdown del diccionario, con búsqueda)
  - Veda temporal (días) o permanente
  - Fecha de retorno (calculada, salta fines de semana)
  - Notas
  - Opción de reagendar turno si existe

### Acción
- Botón "GUARDAR DONACIÓN" (o "GUARDAR RESTRICCIONES" en modo restricción).
- **Validaciones:** sin donante → toast; campos clínicos vacíos → toast; sin procedimiento → toast.
- **Advertencias no bloqueantes:** si el donante tiene días de recuperación pendientes o supera el tope anual, muestra dialog "Advertencia" [REGISTRAR / CANCELAR]. El operador decide.
- **Resultado:** donación registrada, `last_donation_date` actualizado, `next_eligible_date` calculado según política de recuperación. Se reconsulta el donante para refrescar badge y conteos.

### "Atendido por"
- Se registra automáticamente el usuario logueado (`staff_user_id`) en cada donación y restricción.

---

## 4. Búsqueda de donantes

- **Pantalla:** Tab "Buscar"
- **Al abrir:** lista todos los donantes (aptos primero, paginado).
- **Datos que ingresa:**
  - Texto libre (DNI, nombre o apellido) — mínimo 2 caracteres, debounce 350 ms.
  - Filtros opcionales: Grupo sanguíneo, Factor (dropdowns con opción "Todos").
- **Resultado:** tarjetas con nombre, teléfono, última donación, grupo/factor, estado APTO/EN_VEDA (con motivo y fin de veda), próximo turno agendado.
- **Acciones por tarjeta:**
  - Tap → carga donante completo → formulario clínico.
  - Botón WhatsApp → confirmación si en veda → abre `wa.me`.
  - Botón "VER DATOS" → formulario en modo solo lectura.
- **Paginación:** scroll al final carga más resultados mientras `hasMore` sea verdadero.

---

## 5. Agenda (turnos)

- **Pantalla:** Tab "Turnos"
- **Vista:** calendario mensual + filtro por día (chips Lun–Vie) + lista de turnos.

### Agendar turno nuevo
- **Botón:** "AGENDAR UN DONANTE"
- **Datos que ingresa:**
  - DNI del donante (búsqueda)
  - Fecha (selector, hoy deshabilitado, solo días hábiles)
  - Hora (selector de hora)
  - Procedimiento (Plasma/Plaquetas/Sangre)
- **Acción:** confirmar → `POST create_appointment`.
- **Resultado:** turno creado con estado SCHEDULED.

### Acciones por turno
- **Tap en card (cuerpo):** abre formulario clínico con donante y procedimiento preseleccionados.
- **Icono WhatsApp:** envía recordatorio por `wa.me` → marca flag `is_reminder_sent`.
- **Icono Email:** envía por `ACTION_SENDTO` → marca flag `is_email_reminder_sent`.
- **Botón Dispatch (envío masivo):** procesa secuencialmente todos los turnos pendientes (ventana 08:00–13:00, anti-ráfaga, retry).

### Recordatorios
- **Manual:** por turno, botones WhatsApp y Email.
- **Masivo:** botón dispatch.
- **Automático:** scheduler diario 08:00 (tarea `RecordatoriosDonacion`) + lazy sweep.

---

## 6. Perfil de usuario

- **Pantalla:** "Mi Perfil" (botón en dashboard para directoras, botón top-right para administrativas).
- **Datos editables:** nombre, apellido, email, teléfono.
- **Datos read-only:** rol, centro.
- **Acción:** botón "GUARDAR" → `POST profile.php`.

---

## 7. Administración (Directora / Superadmin)

### 7.1 Usuarios
- **Tabs:** Pendientes / Activos.
- **Aprobar:** seleccionar rol (Administrativa/Directora/Superadmin) + centro → "APROBAR".
- **Cambiar rol:** seleccionar nuevo rol → confirmación si es Directora/Superadmin.
- **Eliminar:** confirmación con mutex (no se puede eliminar al último Director/Superadmin del centro).

### 7.2 Restricciones (diccionario)
- **Crear:** nombre + días de veda default.
- **Eliminar:** desaparece del diccionario.
- Se usa en el formulario clínico (dropdown de restricciones).

### 7.3 Políticas de recuperación
- **CRUD** por procedimiento (PLASMA/PLATELETS/BLOOD): días de recuperación y tope anual (hombre/mujer).
- Baseline INCUCAI/SAS sembrado en la BD.

### 7.4 Estadísticas
- **Rangos:** Día / Semana / Mes / Año.
- **Métricas:** total donaciones, por procedimiento, donantes registrados, donantes en veda.
- **Gráfico:** tendencia (MPAndroidChart).
- **Filtros:** por centro y por staff ("atendido por").

### 7.5 Centros (solo Superadmin)
- **ABM:** crear, editar, activar/desactivar centros.
- **Selector:** cambia el centro activo (filtra todas las requests).

---

## 8. Roles y permisos

| Rol | Pantalla inicial | Capacidades |
|---|---|---|
| **SUPERADMIN** | Dashboard (5 cards + selector centro) | Todo + ABM centros + vista global |
| **DIRECTORA** | Dashboard (5 cards) | Donaciones, Estadísticas, Restricciones, Políticas, Usuarios |
| **ADMINISTRATIVA** | MainActivity (3 tabs) | Alta donante, Búsqueda, Agenda, Formulario clínico |

---

## Referencias

| Módulo | Archivo principal |
|---|---|
| Login | `views/auth/LoginFragment.java` |
| Dashboard | `views/DirectorDashboardActivity.java` |
| Alta donante | `views/fragments/DonorFormFragment.java` |
| Búsqueda | `views/fragments/SearchFragment.java` |
| Formulario clínico | `views/fragments/DonationsFragment.java` |
| Agenda | `views/fragments/AppointmentsFragment.java` |
| Perfil | `views/fragments/ProfileFragment.java` |
| Usuarios | `views/fragments/UsersManagementFragment.java` |
| Restricciones | `views/fragments/RestrictionsManagementFragment.java` |
| Políticas | `views/fragments/RecoveryPoliciesFragment.java` |
| Estadísticas | `views/fragments/StatisticsFragment.java` |
| Navegación | `views/MainActivity.java` |
| Backend donantes | `backend/donors.php` |
| Backend donaciones | `backend/donations.php` |
| Backend agenda | `backend/appointments.php` |
| Backend estadísticas | `backend/statistics.php` |
| Backend usuarios | `backend/users_admin.php` |
| Backend perfil | `backend/profile.php` |
| Backend restricciones | `backend/restrictions.php` |
| Backend políticas | `backend/recovery_policies.php` |
