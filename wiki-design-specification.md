# UI/UX Technical Specification - Mobile Donation App (Java / XML)

This document serves as the official technical specification and design guide for generating **Android XML Layouts and Java Fragments** using **Material Design 3 (Material Components)**. The application is optimized for mobile devices and tailored for clinical staff at a blood and apheresis donation center.

---

## 🎨 General UI/UX Guidelines

- **Visual Style:** Clean, clinical, and corporate interface. Color palette based on medical light-blue tones, clinic dark-blues, and light-gray background surfaces (`@color/material_dynamic_neutral95`).
- **Language Convention:** **CRITICAL RULE.** All internal code, class names, file names, variables, backend logic, and XML element IDs must be written strictly in **English** using CamelCase. All user-facing labels, hints, titles, tooltips, and dialogs must be displayed in **Spanish** utilizing externalized string resources.
- **Navigation:** Root layout structured around a `CoordinatorLayout` or `ConstraintLayout` hosting a fixed **`BottomNavigationView`** at the bottom of the screen.
- **Data Input:** Use `TextInputLayout` with `style="@style/Widget.Material3.TextInputLayout.OutlinedBox"` to implement space-saving floating labels.
- **Keyboard Optimization:** Always specify `android:inputType="number"` or `"numberDecimal"` on numerical input fields (ID, Phone, Weight, Height, HTO, Platelets) to trigger the correct software keyboard.
- **List Rendering:** Mandatory usage of `RecyclerView` paired with a `ListAdapter` or `RecyclerView.Adapter` for memory-efficient scrollable lists.

---

### 🔐 AUTHENTICATION MODULE SPECIFICATIONS

#### 1. Screen: User Registration (`SignUpFragment` / `fragment_sign_up.xml`)
Form engineered to capture and persist newly registered operational staff credentials.
- **Top Brand Anchor:** Centralized institutional graphic icon paired with a prominent header `TextView` displaying `"Crear Cuenta"`.
- **Form Layout (`ScrollView` + vertical `LinearLayout`):**
  - `TextInputLayout` + `TextInputEditText` ➔ `@+id/editTextFirstName` (Hint: `"Nombre/s:"`).
  - `TextInputLayout` + `TextInputEditText` ➔ `@+id/editTextLastName` (Hint: `"Apellido/s:"`).
  - `TextInputLayout` + `TextInputEditText` with `android:inputType="number"` ➔ `@+id/editTextDni` (Hint: `"DNI:"`).
  - `TextInputLayout` + `TextInputEditText` with `android:inputType="textEmailAddress"` ➔ `@+id/editTextEmail` (Hint: `"Correo Electrónico:"`).
  - `TextInputLayout` + `TextInputEditText` with `android:inputType="phone"` ➔ `@+id/editTextPhone` (Hint: `"Teléfono:"`).
  - `TextInputLayout` + `TextInputEditText` with password mask and visibility toggling enabled (`app:endIconMode="password_toggle"`) ➔ `@+id/editTextPassword` (Hint: `"Contraseña:"`).
- **Trigger Component:** Full-width filled `MaterialButton` ➔ `@+id/buttonSignUp` (Displays: `"REGISTRARSE"`).
- **Navigation Footer:** A low-hierarchy `MaterialButton` (TextButton style) ➔ `@+id/buttonNavigateToLogin` (Displays: `"¿Ya tenés cuenta? Iniciar Sesión"`).

#### 2. Screen: User Authentication Login (`LoginFragment` / `fragment_login.xml`)
Core gatekeeping gateway utilizing credentials previously generated in the system.
- **Top Branding Layout:** High-visibility logo and greeting `TextView` displaying `"¡Bienvenido de nuevo!"`.
- **Form Elements:**
  - `TextInputLayout` + `TextInputEditText` with `android:inputType="textEmailAddress"` ➔ `@+id/editTextLoginEmail` (Hint: `"Correo Electrónico:"`).
  - `TextInputLayout` + `TextInputEditText` with active password masking toggles ➔ `@+id/editTextLoginPassword` (Hint: `"Contraseña:"`).
- **Core Form Action:** Prominent filled `MaterialButton` ➔ `@+id/buttonLogin` (Displays: `"INICIAR SESIÓN"`).
- **Navigation Footer:** Secondary plano text utility button ➔ `@+id/buttonNavigateToSignUp` (Displays: `"¿No tenés cuenta? Registrate acá"`).


## 🏛️ Application Architecture & Navigation

The main activity (`MainActivity`) must include a **`BottomNavigationView`** inflated via a menu resource file (`res/menu/bottom_nav_menu.xml`) with the following 4 options (IDs in English):

1. **Donors (`@+id/navigationDonors`):** Registration, modification, and consultation of the donor's file.
2. **Donations (`@+id/navigationDonations`):** Clinical logs for current hemogram values, vitals, and procedure selection.
3. **Appointments (`@+id/navigationAppointments`):** Weekly schedule and attendance notification management.
4. **Search (`@+id/navigationSearch`):** Rapid query utility for critical blood stock shortages and specific phenotypes.

Navigation between these views must be handled in Java by swapping **`Fragment`s** within a core `FragmentContainerView` using `SupportFragmentManager`.

---

## 📱 Detailed Screen Specifications

### 1. Screen: Donor Registration & File (`DonorsFragment`)
Handles two logical UI states controlled via Java visibility flags: **Empty Form (Registration Mode)** and **Populated Form (Edit/Consultation Mode)**.

#### XML Layout Structure (`fragment_donors.xml`):
- **Top Search Bar:** An outlined `TextInputLayout` with an embedded magnifying glass icon (`app:startIconDrawable="@drawable/ic_search"`) named `@+id/textInputLayoutSearchDni`.
- **Main Container (`ScrollView` + vertical `LinearLayout`):**
  - **Card View 1: Personal Data** (`MaterialCardView` with ID `@+id/cardPersonalData`)
    - Horizontal `LinearLayout` splitting Last Name (`@+id/editTextLastName`) and First Name (`@+id/editTextFirstName`) using `android:layout_width="0dp"` and `android:layout_weight="1"`. User-facing hints: `"Apellido/s:"` and `"Nombre/s:"`.
    - Horizontal `LinearLayout` pairing National ID (`@+id/editTextDni`, hint: `"DNI:"`) and Gender (implemented via an `AutoCompleteTextView` dropdown inside a `TextInputLayout`, ID `@+id/spinnerGender`, hint: `"Género:"`).
    - Horizontal `LinearLayout` for Birth Date (`@+id/editTextBirthDate` - triggers a `MaterialDatePicker` via Java listener) and Age (`@+id/editTextAge` set to `android:enabled="false"`).
  - **Card View 2: Contact and Location** (`@+id/cardContactData`)
    - Vertically stacked fields for City (`@+id/editTextCity`), Zip Code (`@+id/editTextZipCode`), Address (`@+id/editTextAddress`), Neighborhood (`@+id/editTextNeighborhood`), Phone (`@+id/editTextPhone`, hint: `"Teléfono:"`), and Email (`@+id/editTextEmail`).
  - **Card View 3: Critical Data** (`@+id/cardCriticalData`)
    - `TextInputLayout` containing an `AutoCompleteTextView` dropdown to select Blood Type and Factor (`@+id/spinnerBloodType`, hint: `"Grupo y Factor:"`).
    - `TextInputLayout` for entering extended Phenotypes (`@+id/editTextPhenotype`, example hint: `"Fenotipo (ej: Kell-):"`).
    - Read-only field displaying the historical last donation timestamp (`@+id/editTextLastDonationDate`, hint: `"Última Donación:"`).
    - **Aptitude Badge (`TextView` with ID `@+id/textViewStatusBadge`):** A custom shape component with rounded corners. It must change dynamically via Java logic: Green background with white text displaying `"APTO PARA DONAR"`, or Red background with white text displaying `"EN VEDA"`.

#### Lower Sticky Action Buttons:
- **Registration Mode:** A single full-width filled `MaterialButton` with ID `@+id/buttonSaveDonor` mapped to string `@string/btn_save_donor` (Displays: `"CARGAR PACIENTE"`).
- **Consultation Mode:** A horizontal `LinearLayout` pairing an outlined button style `style="@style/Widget.Material3.Button.OutlinedButton"` (`@+id/buttonModifyDonor`, displays: `"MODIFICAR"`) right next to the main confirmation button.

---

### 2. Screen: New Donation Entry & Results (`DonationsFragment`)
Captures daily clinical measurements linked to an active, validated donor.

#### XML Layout Structure (`fragment_donations.xml`):
- **Donor Context Card (`MaterialCardView` top-docked with ID `@+id/cardDonorContext`):** A subtle gray block housing fixed `TextView` items displaying the selected donor's Full Name, ID, and Blood Type as a safety reference header.
- **Clinical Form Elements:**
  - **Vitals Block:** A horizontal row containing two compact `TextInputLayout` items for Weight (`@+id/editTextWeight`, hint: `"Peso (kg):"`) and Height (`@+id/editTextHeight`, hint: `"Altura (cm):"`).
  - **Short Hemogram Block:** A `MaterialCardView` (`@+id/cardHemogram`) wrapping the two mandatory clinic metrics:
    - `TextInputLayout` with `android:inputType="numberDecimal"` targeting Hematocrit (`@+id/editTextHto`, hint: `"HTO (%):"`).
    - `TextInputLayout` with `android:inputType="number"` targeting Platelet Count (`@+id/editTextPlateletsCount`, hint: `"Recuento de Plaquetas (u/µL):"`).
  - **Procedure Type Selection (Multiple Choice/Apheresis Combos):** 
    Must implement a **`MaterialButtonToggleGroup`** (`@+id/toggleGroupDonationType`) containing three buttons: Plasma (`@+id/buttonTypePlasma`, text: `"Plasma"`), Platelets (`@+id/buttonTypePlatelets`, text: `"Plaquetas"`), and Whole Blood (`@+id/buttonTypeBlood`, text: `"Sangre"`).
    *XML Configuration:* The single-selection restriction attribute **must be disabled** (`app:singleSelection="false"`) to allow concurrent apheresis options (e.g., extracting both Plasma + Platelets in a single session).
    *Java Mutex Logic Requirement:* Whole Blood is strictly mutually exclusive with apheresis procedures. In Java, checking `@+id/buttonTypeBlood` must automatically clear selection for Plasma and Platelets. Conversely, selecting Plasma or Platelets must automatically uncheck Whole Blood.
- **Core Form Action:** A full-width `MaterialButton` placed at the bottom layout threshold: `@+id/buttonSaveDonation` referencing resource `@string/btn_save_donation` (Displays: `"GUARDAR DONACIÓN"`).

---

### 3. Screen: Weekly Schedule Agenda (`AppointmentsFragment`)
Monitors upcoming and active patient reservations.

#### XML Layout Structure (`fragment_appointments.xml`):
- **Agenda Header:** A clean `TextView` (`@+id/textViewWeekHeader`) outlining the current operating date bounds (e.g., `"Citados para la Semana del 13 al 19 de Mayo"`).
- **Horizontal Day Filter:** A horizontal scrollable `RecyclerView` (`@+id/recyclerViewDaysFilter`, `android:orientation="horizontal"`) hosting small cards for week navigation (e.g., `"Lun 13"`, `"Mar 14"`, `"Mié 15"`). Tapping a cell acts as an instantaneous query filter for the list below.
- **Appointments Feed:** A vertical `RecyclerView` (`@+id/recyclerViewAppointmentsList`) rendering an individual card item file (`item_appointment.xml`) for each targeted donor.
  - **item_appointment.xml Architecture:** A `MaterialCardView` using a `ConstraintLayout` to space out the appointment Time stamp, Donor Full Name, intended procedure tags, an embedded custom outlined button with a messaging vector icon (`@+id/buttonSendReminder`, text: `"Enviar Recordatorio"`), and a success check asset (`@+id/imageViewReminderChecked`, hidden by default).

#### ✉ WHATSAPP REMINDER OPTIMIZATION RULE
- **Intent Execution:** Pressing `@+id/buttonSendReminder` must construct a dynamic string pulling fields from the targeted row (`donorName`, `appointmentTime`, `donationType`).
- **Formatting:** Use asterisks `*` in the string context to enforce native WhatsApp bold lettering on key parameters (e.g., `"Hola *%s*, te recordamos tu turno para donación de *%s* a las *%s* hs."`).
- **State Persistence:** Upon button click, toggle a local database flag `isReminderSent = true`. The `RecyclerView` adapter must immediately re-bind the view, changing the button color or replacing it with a success check asset (`@+id/imageViewReminderChecked`) to provide visual tracking to the staff.

---

### 4. Screen: Critical Search Lookup (`SearchFragment`)
Contingency utility engineered to fast-track donor engagement during emergency blood stock deficits.

#### XML Layout Structure (`fragment_search.xml`):
- **Primary Filter Element:** High-visibility drop-down selector implementing `TextInputLayout` with an inner `AutoCompleteTextView` to scope critical target blood types (`@+id/spinnerSearchBloodType`, hint: `"Grupo y Factor:"`).
- **Trigger Component:** Prominent filled `MaterialButton`: `@+id/buttonSearchDonors` (Displays: `"BUSCAR DONANTES"`).
- **Query Output Feed:** A vertical `RecyclerView` (`@+id/recyclerViewSearchResults`) inflating standardized search cards (`item_search_result.xml`).
  - **Card Metrics:** Each list entry includes an absolute circular avatar representing the patient's initial, full name typography, phone number string, and historical date tracking their last donation.
  - **Instant Dialer:** An explicit **`MaterialButton` styled as an Icon** (`@+id/buttonCallDonor`) hosting a telephone graphic (`Icons.Default.Phone`) anchored to the right border. Clicking this element must instantly fire an `Intent.ACTION_DIAL` string parameter carrying the target phone value to trigger the device dialer interface instantly.

 ### 💡 UPDATED REQUIREMENT: Donor Suitability & Restrictions

#### 1. Restriction Management System (DonorsFragment / Database)
- **Database Architecture:** Implement a pre-populated `restrictions` table storing canonical medical deferral codes (e.g., "Recent Tattoo", "Recent Surgery", "Medication Block").
- **UI Component Selection:** Implement a multi-select **`AutoCompleteTextView`** embedded within a `TextInputLayout` named `@+id/spinnerMedicalRestrictions`. This component displays the pre-loaded dictionary to let users check multiple active restrictions.
- **Custom Notes Support:** Position a multiline `TextInputLayout` directly below named `@+id/editTextRestrictionNotes` (Hint: "Observaciones de la exclusión:"). This field captures free-text specifications for unique edge cases and stores them as an operational log string.

### 👑 DIRECTOR ROLE & ADMINISTRATIVE MODULES

#### 1. Screen: Director Core Dashboard (`DirectorDashboardActivity` / `activity_director_dashboard.xml`)
Central routing hub displayed exclusively for users authenticated with the "DIRECTOR" role string.
- **Top Welcome Header:** Static colored banner featuring user credentials and title status.
- **Navigation Grid Layout (2x2 Grid using LinearLayouts/GridLayout):**
  - Card 1 ➔ `@+id/cardMenuDonations` (Triggers navigation to the standard 4-item core workflow).
  - Card 2 ➔ `@+id/cardMenuStatistics` (Navigates to data analytics charts view).
  - Card 3 ➔ `@+id/cardMenuRestrictions` (Navigates to medical restrictions CRUD view).
  - Card 4 ➔ `@+id/cardMenuUsers` (Navigates to staff users CRUD panel).

#### 2. Screen: Metrics & Analytics View (`StatisticsFragment` / `fragment_statistics.xml`)
Provides data visualization tools separated into time bounds.
- **Top Switcher:** `TabLayout` bound to a `ViewPager2` providing 4 specific historical filter constraints: `[ Día ]`, `[ Semana ]`, `[ Mes ]`, `[ Año ]`.
- **Performance Output Display:** High-visibility analytics scoreboard displaying total counts per donor cell type, followed by an open-source graphic component (`@+id/chartDonationsTrend`) plotting historical performance curves.

#### 3. Screen: Medical Restrictions CRUD Panel (`RestrictionsManagementFragment` / `fragment_restrictions_management.xml`)
Comprehensive database utility to manage clinical deferral types.
- **Top Input Form Panel:** Outlined `TextInputLayout` parameters for custom title inputs (`@+id/editTextNewRestrictionName`) and explicit deferral duration variables (`@+id/editTextVedaDaysCount`). Includes confirmation triggers: `@+id/buttonSaveRestriction`.
- **Bottom Database Feed:** A vertical `RecyclerView` (`@+id/recyclerViewRestrictionsList`) tracking existing parameters. Each individual item row contains an inline context delete vector action asset (`@+id/buttonDeleteRestriction`).

#### 4. Screen: Staff User Accounts CRUD Panel (`UsersManagementFragment` / `fragment_users_management.xml`)
System employee database controller enforcing security isolation and two-step sign-up approval rules.

- **Approval Workflow & Database Flags:** Newly registered users via `SignUpFragment` are persisted by default with `isApproved = false` and `role = "NONE"`. These accounts are blocked from system entry at the login gate until modified by a Director.
- **UI Architecture:** Implement a `TabLayout` sorting accounts into two operational states:
  - **Tab 1: Pending Requests (`@+id/tabPendingUsers`):** Displays a vertical `RecyclerView` for unapproved staff. Each row item card must feature an outlined `TextInputLayout` with an inner `AutoCompleteTextView` role selector dropdown (`@+id/spinnerAssignRole`, options: "Administrativa", "Directora") paired with a high-visibility activation filled button: `@+id/buttonApproveUser` (Displays: "APROBAR").
  - **Tab 2: Active Staff (`@+id/tabActiveUsers`):** Tracks currently authorized system operators. Allows role updates and account revocations via the inline context delete vector action asset (`@+id/buttonDeleteUser`).
- **CRITICAL SECURITY MUTEX ENFORCEMENT:** The delete action component (`@+id/buttonDeleteUser`) must query the local database context before execution. If `targetUser.role == "DIRECTOR"`, the deletion thread must count total alternative active accounts with matching director role tags. If total alternate directory counts equal zero (`directorCount <= 1`), the delete routine must freeze execution, skip database transactions, and pop an active error alert interface blocking the action to prevent absolute system lockout.

---

## 🗄️ DATABASE & RELATIONSHIP ARCHITECTURE SPECIFICATIONS

This section outlines the normalized database schema and mandatory entity relationships required across the application.

### 1. Multiple Contact Channels (1-to-Many Relationships)
To support donors and staff users storing multiple phone numbers and email addresses:

- **`donor_phones` / `user_phones` Tables:**
  - `id` (INT PRIMARY KEY AUTO_INCREMENT)
  - `donor_id` / `user_id` (INT, FOREIGN KEY ON DELETE CASCADE)
  - `phone_number` (VARCHAR(30) NOT NULL)
  - `phone_type` (VARCHAR(20) DEFAULT 'Mobile', e.g., 'Mobile', 'Home', 'Work')
  - `is_primary` (TINYINT(1) DEFAULT 0) — Flag identifying default contact number for WhatsApp/SMS intents and direct calls.

- **`donor_emails` / `user_emails` Tables:**
  - `id` (INT PRIMARY KEY AUTO_INCREMENT)
  - `donor_id` / `user_id` (INT, FOREIGN KEY ON DELETE CASCADE)
  - `email_address` (VARCHAR(150) NOT NULL)
  - `email_type` (VARCHAR(20) DEFAULT 'Personal', e.g., 'Personal', 'Work')
  - `is_primary` (TINYINT(1) DEFAULT 0) — Flag identifying default email.

---

### 2. Appointments & Reminder Tracking Schema
- **`appointments` Table Key Columns:**
  - `id` (INT PRIMARY KEY AUTO_INCREMENT)
  - `donor_id` (INT, FOREIGN KEY referencing `donors(id)` ON DELETE CASCADE)
  - `appointment_date` (DATE NOT NULL)
  - `appointment_time` (TIME NOT NULL)
  - `target_procedure` (VARCHAR(50) NOT NULL, e.g., 'Plasma', 'Platelets', 'Blood')
  - `status` (VARCHAR(20) DEFAULT 'SCHEDULED')
  - `is_reminder_sent` (TINYINT(1) DEFAULT 0) — State persistence flag for WhatsApp reminder tracking.
  - `reminder_sent_at` (DATETIME NULL) — Audit timestamp when reminder was dispatched.
  - `reminder_sent_by_user_id` (INT NULL, FOREIGN KEY referencing `users(id)` ON DELETE SET NULL) — ID of the operational staff member who dispatched the reminder.

---

### 3. Relational Entity Cardinality Map

| Relationship | Type | Parent Table | Child / Junction Table | Purpose & Description |
| :--- | :--- | :--- | :--- | :--- |
| **Donor Contacts** | `1-to-N` | `donors` | `donor_phones`, `donor_emails` | Donors can possess multiple phone numbers and email addresses. |
| **Staff Contacts** | `1-to-N` | `users` | `user_phones`, `user_emails` | Operational staff can possess multiple contact phone numbers and emails. |
| **Donor History** | `1-to-N` | `donors` | `donations` | A donor can perform multiple donation sessions over time. |
| **Donation Extraction Types** | `1-to-N` | `donations` | `donation_procedures` | A single donation session can include concurrent apheresis extractions (e.g., Plasma + Platelets). |
| **Donor Medical Deferrals** | `N-to-M` | `donors` ↔ `restrictions` | `donor_restrictions` | Donors can have multiple active/past deferrals; deferrals apply to many donors. Stores `start_date`, `end_date`, and free-text `notes`. |
| **Donor Appointments** | `1-to-N` | `donors` | `appointments` | A donor can schedule multiple future appointments across calendar weeks. |
| **Staff Clinical Audit** | `1-to-N` | `users` | `donations`, `donor_restrictions` | Tracks which staff member (`staff_user_id`) processed a donation or logged a medical deferral. |
| **Director User Approvals** | `1-to-N` | `users` | `users` (`approved_by_user_id`) | Tracks which Director approved a pending staff account and assigned its operational role. |

