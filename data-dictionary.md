# Diccionario de datos — Donation System

Documento de referencia de todas las tablas y campos de la BD `donation_system`. Complementa `flujo-usuario.md` (flujos) y `wiki-design-specification.md` (UI).

---

## `centers` — Centros de donación

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador único | Sistema |
| `name` | VARCHAR(100) | No | Nombre del centro | ABM centros (superadmin) |
| `address` | VARCHAR(150) | Sí | Dirección | ABM centros |
| `is_active` | TINYINT(1) DEFAULT 1 | No | Centro activo | ABM centros |
| `created_at` | TIMESTAMP | No | Fecha de creación | Sistema |

---

## `users` — Cuentas de staff

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador único | Sistema |
| `first_name` | VARCHAR(100) | No | Nombre | Registro / Perfil |
| `last_name` | VARCHAR(100) | No | Apellido | Registro / Perfil |
| `dni` | VARCHAR(20) UNIQUE | No | DNI | Registro |
| `email` | VARCHAR(150) UNIQUE | No | Email (login) | Registro / Perfil |
| `password_hash` | VARCHAR(255) | No | Hash BCrypt | Registro |
| `role` | ENUM('ADMINISTRATIVA','DIRECTORA','SUPERADMIN') | No | Rol | Aprobación por director/superadmin |
| `is_approved` | TINYINT(1) DEFAULT 0 | No | Cuenta aprobada | Aprobación por director/superadmin |
| `approved_by_user_id` | INT FK→users | Sí | Quién aprobó | Aprobación |
| `center_id` | INT FK→centers | Sí | Centro al que pertenece | Registro / Aprobación |
| `created_at` | TIMESTAMP | No | Fecha de registro | Sistema |

**Relaciones:** 1:N con `user_phones`, `user_emails`. N:1 con `centers`.

---

## `user_phones` — Teléfonos de staff

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `user_id` | INT FK→users | No | Usuario | Registro / Perfil |
| `phone_number` | VARCHAR(30) | No | Número | Registro / Perfil |
| `phone_type` | VARCHAR(20) DEFAULT 'Mobile' | No | Tipo (Mobile/Home/Work) | Registro / Perfil |
| `is_primary` | TINYINT(1) DEFAULT 0 | No | Teléfono principal | Registro / Perfil |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |

---

## `user_emails` — Emails de staff

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `user_id` | INT FK→users | No | Usuario | Registro / Perfil |
| `email_address` | VARCHAR(150) | No | Email | Registro / Perfil |
| `email_type` | VARCHAR(20) DEFAULT 'Personal' | No | Tipo (Personal/Work) | Registro / Perfil |
| `is_primary` | TINYINT(1) DEFAULT 0 | No | Email principal | Registro / Perfil |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |

---

## `donors` — Donantes

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `dni` | VARCHAR(20) UNIQUE | No | DNI | Alta de donante |
| `first_name` | VARCHAR(100) | No | Nombre | Alta de donante |
| `last_name` | VARCHAR(100) | No | Apellido | Alta de donante |
| `gender` | VARCHAR(20) DEFAULT 'Otro' | No | Género | Alta de donante |
| `birth_date` | DATE | No | Fecha de nacimiento | Alta de donante |
| `city` | VARCHAR(100) | No | Ciudad | Alta de donante |
| `address` | VARCHAR(150) | No | Dirección | Alta de donante |
| `blood_type` | VARCHAR(10) | No | Grupo y factor (ej. O+) | Alta de donante |
| `phenotype` | VARCHAR(100) | Sí | Fenotipo (ej. Kell-) | Alta de donante |
| `weight` | DECIMAL(5,2) | Sí | Peso en kg | Alta de donante (datos críticos) |
| `height` | DECIMAL(5,2) | Sí | Altura en cm | Alta de donante (datos críticos) |
| `center_id` | INT FK→centers | Sí | Centro de registro | Automático (del usuario logueado) |
| `last_donation_date` | DATE | Sí | Última donación | Se actualiza al registrar donación |
| `next_eligible_date` | DATE | Sí | Próxima fecha apta (recuperación) | Se calcula al registrar donación |
| `created_at` | TIMESTAMP | No | Fecha de alta | Sistema |

**Nota:** `status` (APTO/EN_VEDA) **no se persiste** — se deriva en cada lectura por el backend a partir de restricciones activas y `next_eligible_date`.

**Relaciones:** 1:N con `donor_phones`, `donor_emails`, `donations`, `donor_restrictions`, `appointments`. N:1 con `centers`.

---

## `donor_phones` — Teléfonos de donante

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `donor_id` | INT FK→donors | No | Donante | Alta de donante |
| `phone_number` | VARCHAR(30) | No | Número | Alta de donante |
| `phone_type` | VARCHAR(20) DEFAULT 'Mobile' | No | Tipo (Mobile/Home/Work) | Alta de donante |
| `is_primary` | TINYINT(1) DEFAULT 0 | No | Teléfono principal (para WhatsApp) | Alta de donante |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |

---

## `donor_emails` — Emails de donante

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `donor_id` | INT FK→donors | No | Donante | Alta de donante |
| `email_address` | VARCHAR(150) | No | Email | Alta de donante |
| `email_type` | VARCHAR(20) DEFAULT 'Personal' | No | Tipo (Personal/Work) | Alta de donante |
| `is_primary` | TINYINT(1) DEFAULT 0 | No | Email principal | Alta de donante |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |

---

## `restrictions` — Diccionario de restricciones médicas

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `name` | VARCHAR(100) UNIQUE | No | Nombre (ej. "Tatuaje Reciente") | ABM restricciones (Directora) |
| `default_veda_days` | INT DEFAULT 0 | No | Días de veda por defecto | ABM restricciones |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |

**Seed inicial:** Tatuaje Reciente (180), Cirugía Reciente (365), Bloqueo por Medicación (30), Viaje a Zona Endémica (90).

---

## `donor_restrictions` — Vedas médicas asignadas a donantes

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `donor_id` | INT FK→donors | No | Donante | Formulario clínico (restricciones) |
| `restriction_id` | INT FK→restrictions | No | Tipo de restricción | Formulario clínico |
| `start_date` | DATE | No | Fecha de inicio de la veda | Formulario clínico |
| `veda_days` | INT | Sí | Días de veda (NULL si permanente) | Formulario clínico |
| `is_permanent` | TINYINT(1) DEFAULT 0 | No | Veda permanente | Formulario clínico |
| `calculated_end_date` | DATE | Sí | Fecha fin calculada | Calculado por backend |
| `eligible_workday_return_date` | DATE | Sí | Fecha de retorno hábil (salta fines de semana) | Calculado por backend |
| `notes` | TEXT | Sí | Notas | Formulario clínico |
| `created_by_user_id` | INT FK→users | Sí | Staff que registró ("atendido por") | Sesión del usuario logueado |
| `center_id` | INT FK→centers | Sí | Centro | Automático (del donante) |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |

---

## `donation_recovery_policies` — Políticas de recuperación por procedimiento

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `procedure_type` | ENUM('PLASMA','PLATELETS','BLOOD') UNIQUE | No | Tipo de procedimiento | ABM políticas (Directora) |
| `recovery_days` | INT | No | Días de recuperación (hombre) | ABM políticas |
| `recovery_days_female` | INT | No | Días de recuperación (mujer) | ABM políticas |
| `max_per_year` | INT | No | Tope anual (hombre) | ABM políticas |
| `max_per_year_female` | INT | No | Tope anual (mujer) | ABM políticas |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |
| `updated_at` | TIMESTAMP | No | Última modificación | Sistema |

**Baseline (INCUCAI/SAS):** PLASMA 2d/25año, PLATELETS 7d/12año, BLOOD 60d hombre (4/año) / 90d mujer (3/año).

---

## `donations` — Donaciones registradas

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `donor_id` | INT FK→donors | No | Donante | Formulario clínico |
| `weight` | DECIMAL(5,2) DEFAULT 0 | No | Peso snapshot (kg) | Formulario clínico (del donante) |
| `height` | DECIMAL(5,2) DEFAULT 0 | No | Altura snapshot (cm) | Formulario clínico (del donante) |
| `hto` | DECIMAL(4,1) | No | Hematocrito (%) | Formulario clínico |
| `platelets_count` | INT | No | Recuento de plaquetas (u/µL) | Formulario clínico |
| `annual_limit_override` | TINYINT(1) DEFAULT 0 | No | Superó tope anual (advertencia) | Calculado por backend |
| `staff_user_id` | INT FK→users | Sí | Staff que registró ("atendido por") | Sesión del usuario logueado |
| `center_id` | INT FK→centers | Sí | Centro | Automático (del donante/usuario) |
| `created_at` | TIMESTAMP | No | Fecha y hora de registro | Sistema |

**Relaciones:** N:1 con `donors`, `users` (staff), `centers`. 1:N con `donation_procedures`.

---

## `donation_procedures` — Procedimientos por donación

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `donation_id` | INT FK→donations | No | Donación | Formulario clínico |
| `procedure_type` | ENUM('PLASMA','PLATELETS','BLOOD') | No | Tipo de extracción | Formulario clínico (toggle) |

---

## `appointments` — Turnos de la Agenda

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK AUTO_INCREMENT | No | Identificador | Sistema |
| `donor_id` | INT FK→donors | No | Donante | Agendar turno |
| `appointment_date` | DATE | No | Fecha del turno | Agendar turno |
| `appointment_time` | TIME | No | Hora del turno | Agendar turno |
| `target_procedure` | VARCHAR(50) | No | Procedimiento (Plasma/Plaquetas/Sangre) | Agendar turno |
| `status` | ENUM('SCHEDULED','ATTENDED','CANCELLED') DEFAULT 'SCHEDULED' | No | Estado del turno | Sistema / cancelación |
| `is_reminder_sent` | TINYINT(1) DEFAULT 0 | No | Recordatorio WhatsApp enviado | Envío de recordatorio |
| `is_email_reminder_sent` | TINYINT(1) DEFAULT 0 | No | Recordatorio email enviado | Envío de recordatorio |
| `reminder_sent_at` | DATETIME | Sí | Fecha/hora de envío del recordatorio | Envío de recordatorio |
| `reminder_sent_by_user_id` | INT FK→users | Sí | Staff que envió el recordatorio | Envío de recordatorio |
| `center_id` | INT FK→centers | Sí | Centro | Automático (del donante) |
| `created_at` | TIMESTAMP | No | Fecha de creación | Sistema |

---

## `reminder_dispatch_control` — Guard del dispatcher automático

| Campo | Tipo | Nullable | Descripción | Origen |
|---|---|---|---|---|
| `id` | INT PK DEFAULT 1 | No | Siempre 1 (single-row) | Sistema |
| `last_run_at` | DATETIME | Sí | Última ejecución del sweep | Sistema |
| `warmup_start_date` | DATE | Sí | Inicio del warm-up (tope diario) | Sistema |
| `created_at` | TIMESTAMP | No | Fecha | Sistema |
| `updated_at` | TIMESTAMP | No | Última modificación | Sistema |

---

## Relaciones (resumen)

| Relación | Tipo | Tablas |
|---|---|---|
| Centro → Usuarios | 1:N | `centers` → `users` |
| Centro → Donantes | 1:N | `centers` → `donors` |
| Centro → Donaciones | 1:N | `centers` → `donations` |
| Centro → Turnos | 1:N | `centers` → `appointments` |
| Centro → Restricciones de donante | 1:N | `centers` → `donor_restrictions` |
| Usuario → Teléfonos/Emails | 1:N | `users` → `user_phones`, `user_emails` |
| Donante → Teléfonos/Emails | 1:N | `donors` → `donor_phones`, `donor_emails` |
| Donante → Donaciones | 1:N | `donors` → `donations` |
| Donación → Procedimientos | 1:N | `donations` → `donation_procedures` |
| Donante ↔ Restricciones | N:M | `donors` ↔ `restrictions` via `donor_restrictions` |
| Donante → Turnos | 1:N | `donors` → `appointments` |
| Staff → Donaciones (atendido por) | 1:N | `users` → `donations` (`staff_user_id`) |
| Staff → Restricciones (creado por) | 1:N | `users` → `donor_restrictions` (`created_by_user_id`) |
| Staff → Turnos (recordatorio enviado por) | 1:N | `users` → `appointments` (`reminder_sent_by_user_id`) |
| Director → Aprobación de usuarios | 1:N | `users` → `users` (`approved_by_user_id`) |
