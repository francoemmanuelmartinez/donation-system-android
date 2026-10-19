# Flujo de usuario — Módulo Donaciones

Diagrama de flujo de las pantallas por las que pasa el operador al registrar una donación.

```mermaid
flowchart TD
    A[MainActivity<br/>Bottom nav: Registrar Donante] --> C[DonorFormFragment<br/>alta de donante (raíz del tab)]
    A -->|"tab Buscar"| D["SearchFragment<br/>(búsqueda unificada)"]

    %% ---------- Alta de donante ----------
    subgraph ALTA [Alta de donante]
        C --> C1[Formulario: datos personales,<br/>contacto, críticos]
        C1 -->|"CARGAR DONANTE"| C2{POST donors.php}
        C2 -->|ERROR| C3["Toast error<br/>permanecer en formulario"]
        C2 -->|OK| C5[Recarga donante por DNI]
        C5 --> E[openClinicalForm]
    end

    %% ---------- Búsqueda ----------
    subgraph BUSQUEDA [Búsqueda de donante (módulo unificado)]
        D --> D0["Apertura: lista todos los donantes<br/>(aptos primero, paginado)<br/>GET search.php"]
        D0 --> D1["Texto: DNI, nombre o apellido (debounce ≥2)<br/>y/o filtros Grupo/Factor → GET search.php"]
        D1 --> D2{¿Resultados?}
        D2 -->|No| D3["Toast: no se encontraron donantes"]
        D3 --> D1
        D2 -->|Sí| D4["Tarjetas con estado APTO / EN VEDA<br/>+ motivo/turno; scroll → load more (hasMore)"]
        D4 -->|tap en tarjeta| D5["GET donors.php<br/>carga donante completo"]
        D4 -->|botón WhatsApp| D6["Confirmar (si en veda) y abrir wa.me"]
        D5 --> E
    end

    %% ---------- Formulario clínico ----------
    subgraph CLINICO [Formulario de donación]
        E["openClinicalForm<br/>raíz = alta de donante →<br/>push DonationsFragment"]
        E --> F[DonationsFragment]
        F --> F1["Cabecera: nombre, DNI,<br/>grupo, badge de estado<br/>VER DATOS → DonorFormFragment solo lectura"]
        F1 --> F2[Hemograma: HTO, recuento de plaquetas]
        F2 --> F3["Toggle procedimiento<br/>Plasma / Plaquetas / Sangre (exclusivo)"]

        F3 -->|"GUARDAR DONACIÓN"| G{Validaciones}
        G -->|"sin donante"| G1[Toast: "Seleccioná un donante primero"]
        G -->|"campos clínicos vacíos"| G2[Toast: "Por favor completá todos los parámetros clínicos"]
        G -->|"sin procedimiento"| G3[Toast: "Seleccioná al menos un tipo de extracción"]
        G -->|"OK"| H{¿Advertencias<br/>de recuperación?}

        H -->|"Próxima fecha futura<br/>o tope anual superado"| I["Dialog: Advertencia<br/>[REGISTRAR / CANCELAR]"]
        I -->|CANCELAR| F3
        I -->|REGISTRAR| J
        H -->|"sin advertencias"| J[POST donations.php]

        J -->|ERROR| K[Toast error<br/>botón habilitado]
        J -->|OK| L["Toast: donación registrada<br/>+ limpiar formulario"]
        L --> M["Recarga donante (nextEligibleDate,<br/>annualCounts, badge) →<br/>SharedDonorViewModel"]
        M --> F3

        %% ---- Restricciones (movidas desde el alta) ----
        F1 --> RF["Card Restricciones:<br/>lista de restricciones activas<br/>EDITAR / QUITAR"]
        RF -->|"Editar"| RE["Dialog: restricción, veda<br/>(días o permanente), notas<br/>+ opción de ajustar turno"]
        RF -->|"QUITAR"| RR["POST restrictions.php revoke<br/>(quita la veda)"]
        F3 --> RC["Checkbox: Aplicar restricciones (No apto)"]
        RC -->|"marcado"| RCA["Captura de restricción + veda<br/>+ fecha de retorno editable<br/>botón → GUARDAR RESTRICCIONES<br/>(oculta carga de donación)"]
        RCA -->|"GUARDAR RESTRICCIONES"| RS{¿Agendar/reagendar turno?}
        RS -->|"No"| RA["POST restrictions.php<br/>assign / update"]
        RS -->|"Sí"| RQ["POST restrictions.php reschedule:<br/>cancela turno actual +<br/>crea turno nuevo (transacción)"]
        RQ --> RA
    end

    K --> F3
    C3 --> C1

    style C fill:#e3f2fd,stroke:#1565c0
    style E fill:#fff3e0,stroke:#ef6c00
    style I fill:#ffebee,stroke:#c62828
    style L fill:#e8f5e9,stroke:#2e7d32
```

## Puntos clave

- **Entradas que convergen en `MainActivity.openClinicalForm()` (MainActivity.java:81)**: el **tab Registrar Donante** (tras guardar el alta), el **tab Buscar** (tap en una tarjeta) y **Agenda** (tap en un turno). Todas dejan el **alta de donante** como raíz del tab y apilan `DonationsFragment`; el back desde el form clínico vuelve al alta vacía.
- **Búsqueda unificada**: `SearchFragment` (tab *Buscar*) reemplaza al antiguo `DonorSearchFragment`. Un único endpoint `search.php` resuelve texto libre (DNI/nombre/apellido), filtros Grupo/Factor, estado calculado en SQL y paginación (`hasMore`). Un donante existente se carga desde ahí (tap en la tarjeta).
- **`VER DATOS`** abre `DonorFormFragment` en modo solo lectura (`openDonorDetails()`) sin salir del formulario clínico.
- **Las restricciones médicas se gestionan en `DonationsFragment`** (ya no en el alta): el alta siempre registra al donante como APTO. Desde el formulario clínico se aplican con el checkbox **"Aplicar restricciones (No apto)"** (`assign`/`update` + veda temporal o **permanente**), y la card de *Restricciones activas* permite **EDITAR** (in-place) y **QUITAR** (`revoke`). Si hay un turno en la Agenda, se ofrece **reagendar** (cancela el turno tipeado y crea uno nuevo en la fecha de retorno de veda, en una sola transacción).
- **Las advertencias son no bloqueantes**: el dialog solo permite continuar igual — la decisión queda en manos del operador. Se disparan cuando `next_eligible_date` es futura o cuando el conteo anual de donaciones alcanza/supera el tope de la política de recuperación (políticas configurables por la Directora desde `recovery_policies.php`; baseline INCUCAI/SAS sembrada en `sql/donation_system.sql`).
- **Tras guardar la donación** se reconsulta el donante por DNI para refrescar `nextEligibleDate`, `annualCounts` y el badge de estado en `SharedDonorViewModel`.
- **Orden de estados en el badge**: `EN_VEDA` si hay restricción médica activa **o** si `next_eligible_date` es futura; en caso contrario `APTO`.

## Referencias

| Pantalla | Archivo |
|---|---|
| Alta de donante (raíz del tab Registrar Donante) | `app/src/main/java/com/francomartinez/donationsystem/views/fragments/DonorFormFragment.java` |
| Búsqueda unificada | `app/src/main/java/com/francomartinez/donationsystem/views/fragments/SearchFragment.java` |
| Formulario de donación | `app/src/main/java/com/francomartinez/donationsystem/views/fragments/DonationsFragment.java` |
| Navegación | `app/src/main/java/com/francomartinez/donationsystem/views/MainActivity.java` |
