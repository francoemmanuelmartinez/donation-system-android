# ERD — Donation System

Diagrama Entidad-Relación (Mermaid `erDiagram`) de la base de datos `donation_system`. Complementa `data-dictionary.md` (detalle de campos) y `casos-de-uso.md` (actores y acciones).

```mermaid
erDiagram
    centers ||--o{ users : "tiene staff"
    centers ||--o{ donors : "registra donantes"
    centers ||--o{ donations : "registra donaciones"
    centers ||--o{ appointments : "agenda turnos"
    centers ||--o{ donor_restrictions : "registra vedas"

    users ||--o{ user_phones : "tiene telefonos"
    users ||--o{ user_emails : "tiene emails"
    users ||--o{ donations : "atiende (staff_user_id)"
    users ||--o{ donor_restrictions : "crea (created_by_user_id)"
    users ||--o{ appointments : "envia recordatorio (reminder_sent_by_user_id)"
    users ||--o{ users : "aprueba (approved_by_user_id)"

    donors ||--o{ donor_phones : "tiene telefonos"
    donors ||--o{ donor_emails : "tiene emails"
    donors ||--o{ donations : "realiza"
    donors ||--o{ donor_restrictions : "tiene vedas"
    donors ||--o{ appointments : "tiene turnos"

    restrictions ||--o{ donor_restrictions : "tipo de veda"

    donations ||--o{ donation_procedures : "incluye procedimientos"

    centers {
        int id PK
        varchar name
        varchar address
        tinyint is_active
        timestamp created_at
    }

    users {
        int id PK
        varchar first_name
        varchar last_name
        varchar dni UK
        varchar email UK
        varchar password_hash
        enum role
        tinyint is_approved
        int approved_by_user_id FK
        int center_id FK
        timestamp created_at
    }

    user_phones {
        int id PK
        int user_id FK
        varchar phone_number
        varchar phone_type
        tinyint is_primary
        timestamp created_at
    }

    user_emails {
        int id PK
        int user_id FK
        varchar email_address
        varchar email_type
        tinyint is_primary
        timestamp created_at
    }

    donors {
        int id PK
        varchar dni UK
        varchar first_name
        varchar last_name
        varchar gender
        date birth_date
        varchar city
        varchar address
        varchar blood_type
        varchar phenotype
        decimal weight
        decimal height
        int center_id FK
        date last_donation_date
        date next_eligible_date
        timestamp created_at
    }

    donor_phones {
        int id PK
        int donor_id FK
        varchar phone_number
        varchar phone_type
        tinyint is_primary
        timestamp created_at
    }

    donor_emails {
        int id PK
        int donor_id FK
        varchar email_address
        varchar email_type
        tinyint is_primary
        timestamp created_at
    }

    restrictions {
        int id PK
        varchar name UK
        int default_veda_days
        timestamp created_at
    }

    donor_restrictions {
        int id PK
        int donor_id FK
        int restriction_id FK
        date start_date
        int veda_days
        tinyint is_permanent
        date calculated_end_date
        date eligible_workday_return_date
        text notes
        int created_by_user_id FK
        int center_id FK
        timestamp created_at
    }

    donation_recovery_policies {
        int id PK
        enum procedure_type UK
        int recovery_days
        int recovery_days_female
        int max_per_year
        int max_per_year_female
        timestamp created_at
        timestamp updated_at
    }

    donations {
        int id PK
        int donor_id FK
        decimal weight
        decimal height
        decimal hto
        int platelets_count
        tinyint annual_limit_override
        int staff_user_id FK
        int center_id FK
        timestamp created_at
    }

    donation_procedures {
        int id PK
        int donation_id FK
        enum procedure_type
    }

    appointments {
        int id PK
        int donor_id FK
        date appointment_date
        time appointment_time
        varchar target_procedure
        enum status
        tinyint is_reminder_sent
        tinyint is_email_reminder_sent
        datetime reminder_sent_at
        int reminder_sent_by_user_id FK
        int center_id FK
        timestamp created_at
    }

    reminder_dispatch_control {
        int id PK
        datetime last_run_at
        date warmup_start_date
        timestamp created_at
        timestamp updated_at
    }
```

### Notas

- `donors.status` (APTO/EN_VEDA) **no es una columna** — se deriva en cada lectura por el backend (`donors.php`, `search.php`).
- `restrictions` y `donation_recovery_policies` son **globales** (no tienen `center_id`).
- `reminder_dispatch_control` es una tabla single-row (id=1) usada por el lazy sweep.
- Las FKs con `ON DELETE SET NULL` permiten borrar un centro/usuario sin perder datos históricos.