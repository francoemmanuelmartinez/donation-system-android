# UI/UX Technical Specification - Mobile Donation App (Java / XML)

This document serves as the official technical specification and design guide for generating **Android XML Layouts and Java Fragments** using **Material Design 3 (Material Components)**. The application is optimized for mobile devices and tailored for clinical staff at a blood and apheresis donation center.

---

## 🎨 General UI/UX Guidelines

- **Visual Style:** Clean, clinical, and corporate interface. Color palette based on medical light-blue tones, clinic dark-blues, and light-gray background surfaces (`@color/surface_light`).
- **Language Convention:** **CRITICAL RULE.** All internal code, class names, file names, variables, backend logic, and XML element IDs must be written strictly in **English** using CamelCase. All user-facing labels, hints, titles, tooltips, and dialogs must be displayed in **Spanish** utilizing externalized string resources.
- **Navigation:** Root layout structured around a `CoordinatorLayout` or `ConstraintLayout` hosting a fixed **`BottomNavigationView`** at the bottom of the screen.
- **Data Input:** Use `TextInputLayout` with `style="@style/Widget.Material3.TextInputLayout.OutlinedBox"` to implement space-saving floating labels.
- **Keyboard Optimization:** Always specify `android:inputType="number"` or `"numberDecimal"` on numerical input fields (ID, Weight, Height, HTO, Platelets) and `android:inputType="phone"` on phone fields, to trigger the correct software keyboard.
- **Dropdown Selection Rule:** Pure selector dropdowns (`AutoCompleteTextView` inside a `TextInputLayout` with `app:endIconMode="dropdown_menu"`) must set `android:inputType="none"` on the `AutoCompleteTextView` so tapping the field opens the list **without** invoking the soft keyboard. Applies to `spinnerGender`, `spinnerBloodType`, `spinnerSearchGroup` and `spinnerSearchFactor`. The restrictions dictionary `spinnerAddRestriction` on the donations screen also uses `android:inputType="none"` (no soft keyboard; its item listener does not call `KeyboardUtils`). `KeyboardUtils` (`utils/KeyboardUtils.java`) is the helper centralizing `InputMethodManager.hideSoftInputFromWindow`, used e.g. in `SearchFragment` and `ScheduleAppointmentFragment`.
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

The main activity (`MainActivity`) must include a **`BottomNavigationView`** inflated via a menu resource file (`res/menu/bottom_nav_menu.xml`) with the following 3 options (IDs in English):

1. **Register Donor (`@+id/navigationDonations`, label "Registrar Donante"):** Root screen is the donor registration form (`DonorFormFragment`). Saving a donor appends the clinical donation entry (`DonationsFragment`) on top; back returns to an empty registration form.
2. **Appointments (`@+id/navigationAppointments`):** Weekly schedule and reminder management.
3. **Search (`@+id/navigationSearch`):** Unified donor lookup — free text (DNI, name or last name) plus group/factor filters, paginated results, and estado (APTO / EN VEDA). Existing donors are opened from a result card into the clinical donation form.

Navigation between these views must be handled in Java by swapping **`Fragment`s** within a core `FragmentContainerView` using `SupportFragmentManager`.

---

## 📱 Detailed Screen Specifications

### 1. Screen: Donor Registration & File (`DonorFormFragment` / `fragment_donor_form.xml`)
Handles three logical UI states driven by Java (`setMode`): **Registration (`MODE_REGISTER`)**, **Consultation (`MODE_VIEW`)** and **Modification (`MODE_MODIFY`)**. The form is a `ScrollView` hosting three tinted `MaterialCardView` sections with colored header pills, stacked inside a vertical `LinearLayout`.

#### Section Tinting Rule
- **Card 1 - Personal Data (`@+id/cardPersonalData`):** `soft_blue` background with a `bg_header_blue` title pill.
- **Card 2 - Contact and Location (`@+id/cardContactData`):** `soft_blue` background with a `bg_header_blue` title pill.
- **Card 3 - Critical Data (`@+id/cardCriticalData`):** navy background (`medical_blue_dark`) with a white title over a `bg_header_dark` pill.

#### XML Layout Structure (`fragment_donor_form.xml`):
- **Card 1: Personal Data**
  - Horizontal `LinearLayout` splitting Last Name (`@+id/editTextLastName`) and First Name (`@+id/editTextFirstName`) using `layout_weight="1"`. Hints: `"Apellido/s:"` and `"Nombre/s:"`.
  - Horizontal row pairing National ID (`@+id/editTextDni`, `inputType="number"`) and Gender dropdown (`@+id/spinnerGender`, hint `"Género:"`, `android:inputType="none"`).
  - Horizontal row for Birth Date (`@+id/editTextBirthDate`) and Age (`@+id/editTextAge`, disabled). Birth Date opens a `MaterialDatePicker` with a **single tap**: the field is permanently non-focusable (`focusable=false`, `setKeyListener(null)`) and the click listener is attached both to the field and to its container `@+id/textInputLayoutBirthDate` (guarded with `isEnabled()`), so the calendar opens on the first tap.
- **Card 2: Contact and Location**
  - Physical address rows: City (`@+id/editTextCity`), Address (`@+id/editTextAddress`).
  - **Dynamic contact rows** (`@+id/layoutPhoneRows` / `@+id/layoutEmailRows`) inflating `item_contact_row.xml` (input `@+id/editTextRowValue`). Base rows are added automatically (`requestedIfEmptyContacts`). Contact type is hardcoded at save time (`"Mobile"` for phones, `"Personal"` for emails); there are no type dropdowns nor delete buttons on base rows.
  - **Add-row buttons:** `@+id/buttonAddPhone` ("Agregar otro teléfono") and `@+id/buttonAddEmail` ("Agregar otro email"). Rows added through these buttons include a square remove button `@+id/buttonRowRemove` (44dp, `ic_close`, tonal styling, `visibility="gone"` on base rows and hidden in VIEW mode). Using it detaches the whole row container (`LinearLayout` parent).
- **Card 3: Critical Data (Navy "Dark Mode")**
  - All fields use `style="@style/Widget.App.TextInputLayout.NavyField"` (fill `navy_field`, stroke `medical_blue_light` turning white on focus, light hint/label colors) and children render white text via `style="@style/Widget.App.TextInputEditText.NavyField"`. Keeping the fill and the card in the same dark family avoids the floating-label "cut / horizontal line" artifact on dark surfaces.
  - Blood Type dropdown (`@+id/spinnerBloodType`, hint `"Grupo y Factor:"`, `inputType="none"`).
  - Phenotype (`@+id/editTextPhenotype`).
  - Weight / Height row (`@+id/editTextWeight` / `@+id/editTextHeight`, `inputType="numberDecimal"`) — moved from the Donations screen; persisted on `donors` and snapshotted into each `donations` row.
  - Last donation date (`@+id/editTextLastDonationDate`, read-only): its container `@+id/textInputLayoutLastDonationDate` is **hidden (`GONE`) in `MODE_REGISTER`** and visible read-only in `MODE_VIEW` / `MODE_MODIFY`.
  - **Aptitude / restrictions are no longer part of this form** (see §2 `DonationsFragment`): the alta always registers the donor as `APTO`. The former `spinnerAptitude` + `layoutExclusionDetails` block (restriction selector `spinnerAddRestriction`, chips `chipGroupRestrictions`, veda days, notes, return date) was removed from `fragment_donor_form.xml`.
- **Dropdown alignment rule:** every `AutoCompleteTextView` uses `style="@style/Widget.Material3.TextInputEditText.OutlinedBox"` (or the `NavyField` child style) so its internal padding and font size match the regular `TextInputEditText` fields and the text never overflows the container.

#### Lower Sticky Action Buttons:
- `@+id/buttonDonorAction` (full-width, text mapped per mode): in `MODE_VIEW` displays `"MODIFICAR"` and toggles `MODE_MODIFY`; otherwise it saves the donor (register/modify) via `handleSave()`.

---

### 2. Screen: New Donation Entry & Results (`DonationsFragment`)
Captures daily clinical measurements linked to an active, validated donor.

#### XML Layout Structure (`fragment_donations.xml`):
- **Donor Context Card (`MaterialCardView` top-docked with ID `@+id/cardDonorContext`):** A subtle gray block housing fixed `TextView` items displaying the selected donor's Full Name, ID, and Blood Type as a safety reference header.
- **Clinical Form Elements:**
  - **Donor Vitals Snapshot:** Weight and Height are **captured automatically from the donor's file** (`donors.weight` / `donors.height`, entered in the donor form) and copied into the `donations` row as a snapshot at save time. The weight/height inputs no longer exist on this screen. If the donor lacks the data, the snapshot saves `0.0`.
  - **Short Hemogram Block:** A `MaterialCardView` (`@+id/cardHemogram`) wrapping the two mandatory clinic metrics:
    - `TextInputLayout` with `android:inputType="numberDecimal"` targeting Hematocrit (`@+id/editTextHto`, hint: `"HTO (%):"`).
    - `TextInputLayout` with `android:inputType="number"` targeting Platelet Count (`@+id/editTextPlateletsCount`, hint: `"Recuento de Plaquetas (u/µL):"`).
  - **Procedure Type Selection (Multiple Choice/Apheresis Combos):** 
    Must implement a **`MaterialButtonToggleGroup`** (`@+id/toggleGroupDonationType`) containing three buttons: Plasma (`@+id/buttonTypePlasma`, text: `"Plasma"`), Platelets (`@+id/buttonTypePlatelets`, text: `"Plaquetas"`), and Whole Blood (`@+id/buttonTypeBlood`, text: `"Sangre"`).
    *XML Configuration:* The single-selection restriction attribute **must be disabled** (`app:singleSelection="false"`) to allow concurrent apheresis options (e.g., extracting both Plasma + Platelets in a single session).
    *Java Mutex Logic Requirement:* Whole Blood is strictly mutually exclusive with apheresis procedures. In Java, checking `@+id/buttonTypeBlood` must automatically clear selection for Plasma and Platelets. Conversely, selecting Plasma or Platelets must automatically uncheck Whole Blood.
- **Core Form Action:** A full-width `MaterialButton` placed at the bottom layout threshold: `@+id/buttonSaveDonation` referencing resource `@string/btn_save_donation` (Displays: `"GUARDAR DONACIÓN"`).
- **Medical Restrictions Block (moved here from the donor form):**
  - **"Aplicar restricciones (No apto)"** checkbox (`@+id/checkApplyRestrictions`) switches the screen into *restriction mode*: hides the donation capture and relabels the action button to `"GUARDAR RESTRICCIONES"`.
  - Restriction capture mirrors the former exclusion block: dictionary dropdown `@+id/spinnerAddRestriction` (type-to-filter, hides the keyboard via `KeyboardUtils.hide(...)`), veda days `@+id/editTextVedaDays` or a **permanent** checkbox, auto-computed return date `@+id/editTextVedaReturnDate` (skips weekends), multiline notes, and an optional **schedule/reschedule** section.
  - **Active restrictions card** (`@+id/cardRestrictions`): lists active deferrals in the container `@+id/layoutActiveRestrictions` (rows inflated from `item_active_restriction.xml`) with per-item **EDITAR** (in-place `update` through `dialog_restriction_edit.xml`) and **QUITAR** (`revoke`).

---

### 3. Screen: Weekly Schedule Agenda (`AppointmentsFragment` / `fragment_appointments.xml`)
Monitors upcoming and active patient reservations on a **business-week calendar (Monday through Friday)**.

Beyond viewing, the screen offers three actions:
- **Tap on an appointment card** (body of `item_appointment.xml`, not the reminder icons) opens the clinical donation entry (`DonationsFragment` via `MainActivity.openClinicalForm()`) with the appointment's donor preloaded and the appointment's procedure (Sangre/Plasma/Plaquetas) pre-selected (`SharedDonorViewModel`).
- **`@+id/buttonScheduleDonor`** ("AGENDAR UN DONANTE") opens the appointment scheduler (`ScheduleAppointmentFragment`): search donor by DNI → pick a business-day date and time → pick procedure → confirm (`appointments.php` `create_appointment`).
- **`@+id/buttonDispatchReminders`** triggers the mass reminder dispatcher (`appointments.php` `dispatch_reminders` with `manual=true`), which runs sequentially with anti-burst pacing (see `whatsapp-reminders-guide.md` §7).

#### XML Layout Structure (`fragment_appointments.xml`):
- **Agenda Header:** A header row combining a calendar icon button (`@+id/buttonOpenCalendar`, drawable `ic_calendar`), a week header (`@+id/textViewWeekHeader`) and a date label (`@+id/textViewSelectedDateLabel`). The label is `GONE` within the Mon–Fri week (the day filter already shows the current week) and only appears when the selected day falls outside it, formatted as `"Turnos del dd/MM/yyyy"` (`date_label_format`). The same header hosts the mass-reminder dispatcher `@+id/buttonDispatchReminders`.
- **Calendar Navigation (`AppointmentCalendarDialogFragment` / `dialog_appointment_calendar.xml`):** Tapping `buttonOpenCalendar` opens a month dialog with prev/next chevrons (`@+id/buttonPrevMonth` / `@+id/buttonNextMonth`), a month title and a day grid (`MonthCalendarAdapter`, `item_month_day.xml`) marking days that already have scheduled appointments. Selecting a day closes the dialog, switches the agenda to that date and refreshes the list. Any date — including today — can be selected in this month view (the no-same-day rule applies to creating new appointments, not to browsing).
- **Horizontal Day Filter:** A horizontal scrollable `RecyclerView` (`@+id/recyclerViewDaysFilter`, `android:orientation="horizontal"`) hosting small day chips (`DayFilterAdapter`, `item_day_filter.xml`) for the current Mon–Fri week (e.g., `"Lun 13"`, `"Mar 14"`). Tapping a cell applies an instantaneous query filter to the list below and clears the previous selection (`clearSelection()`).
- **Appointments Feed:** A vertical `RecyclerView` (`@+id/recyclerViewAppointmentsList`) rendering an individual card item file (`item_appointment.xml`) for each targeted donor.

#### REMINDER CHANNELS (WHATSAPP & EMAIL) RULES
- **Two independent channels per row:** `@+id/buttonSendWhatsApp` and `@+id/buttonSendEmail`, each with its own success check asset (`@+id/imageViewReminderWhatsAppChecked` / `@+id/imageViewReminderEmailChecked`, hidden by default).
- **WhatsApp Intent:** `wa.me` deep link normalized with the donor's last phone under country prefix `54`; message built from `whatsapp_reminder_format` (donor name, procedure, date, time).
- **Email Intent:** `ACTION_SENDTO` (`mailto:`) composed from `email_reminder_subject` and `email_reminder_format`.
- **Anti double-tap:** each channel button ignores repeated taps while its request is in flight.
- **State persistence:** per-channel database flags `is_reminder_sent` and `is_email_reminder_sent` live on `appointments`. The adapter re-binds immediately after a click, replacing the button with its check image for staff visual tracking.

---

### 4. Screen: Unified Donor Search (`SearchFragment`)
Single, scalable search module (replaces the former `DonorSearchFragment` / critical-stock lookup). On open it lists **all** donors (aptos first, paginated); the operator narrows results with a free-text field and optional filters. One backend endpoint (`search.php`) resolves text, group/factor and pagination; it returns the calculated state per donor but does not accept a state filter parameter.

#### XML Layout Structure (`fragment_search.xml`):
- **Free-text Field:** Outlined `TextInputLayout` with an inner `TextInputEditText` (`@+id/editTextSearchText`, hint `"DNI, nombre o apellido"`, `app:endIconMode="clear_text"`, start icon `@drawable/ic_search`, `imeOptions="actionSearch"`). Debounced (~350 ms); triggers from 2 characters (or when cleared → shows all).
- **Collapsible Filters:** An outlined `MaterialButton` toggle (`@+id/buttonToggleFilters`, "Filtros") reveals `@+id/layoutSearchFilters` (hidden by default) with two `TextInputLayout` + `AutoCompleteTextView` selectors: `@+id/spinnerSearchGroup` (`"Grupo:"`) and `@+id/spinnerSearchFactor` (`"Factor:"`), both `android:inputType="none"` so tapping opens the list without the soft keyboard. Each includes a `"Todos"` option.
- **Query Output Feed:** A vertical `RecyclerView` (`@+id/recyclerViewSearchResults`) inflating the unified donor card (`item_donor_card.xml`, `DonorCardAdapter.Mode.UNIFIED`).
  - **Card content:** avatar inicial, nombre completo, teléfono, última donación, badge de grupo/factor, estado APTO / EN VEDA (con motivo y fin de veda si aplica) y próximo turno agendado.
  - **Actions:** `tap` en la tarjeta carga el donante completo (`GET donors.php`) y abre el formulario clínico (`MainActivity.openClinicalForm()`); el botón de contacto abre WhatsApp (`wa.me`), con diálogo de confirmación si el donante está en veda.
  - **Pagination:** scroll al final dispara "cargar más" mientras `hasMore` sea verdadero.

### 💡 UPDATED REQUIREMENT: Donor Suitability & Restrictions

#### 1. Restriction Management System (DonationsFragment / Database)
- **Database Architecture:** A pre-populated `restrictions` table stores canonical medical deferral codes (Spanish seed: "Tatuaje Reciente", "Cirugía Reciente", "Bloqueo por Medicación", "Viaje a Zona Endémica"); `donor_restrictions` stores each deferral with `veda_days`, `is_permanent`, `calculated_end_date` and `eligible_workday_return_date`.
- **Restriction Flow (moved to `DonationsFragment`):** the donor form no longer captures aptitude/exclusions (it always saves `APTO`). Deferrals are applied from the clinical/donations screen through the **"Aplicar restricciones (No apto)"** checkbox: `POST restrictions.php` `assign`/`update` with a temporal veda (`veda_days` → return date that skips weekends) or a **permanent** veda. Active deferrals are listed in the restrictions card and can be edited in place or removed with **Incluir ahora** (`revoke`). If a scheduled appointment exists, the operator may **reagendar** (the backend cancels the current appointment and creates a new one on the return date in a single transaction). Aptitude/status is derived from active restrictions (a donor with active restrictions is "No apto" / `EN_VEDA`) without mutating a persisted status field.

### 👑 DIRECTOR ROLE & ADMINISTRATIVE MODULES

#### 1. Screen: Director Core Dashboard (`DirectorDashboardActivity` / `activity_director_dashboard.xml`)
Central routing hub displayed exclusively for users authenticated with the `DIRECTORA` / `DIRECTOR` role strings.
- **Top Welcome Header:** Static colored banner featuring user credentials and title status.
- **Navigation Grid Layout (5 cards):**
  - Card 1 ➔ `@+id/cardMenuDonations` (Triggers navigation to the standard 3-tab main workflow).
  - Card 2 ➔ `@+id/cardMenuStatistics` (Navigates to data analytics charts view).
  - Card 3 ➔ `@+id/cardMenuRestrictions` (Navigates to medical restrictions CRUD view).
  - Card 4 ➔ `@+id/cardMenuRecoveryPolicies` (Navigates to the recovery policies CRUD panel).
  - Card 5 ➔ `@+id/cardMenuUsers` (Navigates to staff users CRUD panel).

#### 2. Screen: Metrics & Analytics View (`StatisticsFragment` / `fragment_statistics.xml`, hosted in `StatisticsActivity`)
Provides data visualization tools separated into time bounds.
- **Top Switcher:** `TabLayout` bound to a `ViewPager2` providing 4 specific historical filter constraints: `[ Día ]`, `[ Semana ]`, `[ Mes ]`, `[ Año ]`.
- **Performance Output Display:** High-visibility analytics scoreboard displaying total counts per donor cell type, followed by an open-source graphic component (`@+id/chartDonationsTrend`, in the range view `fragment_statistics_range.xml`) plotting historical performance curves.

#### 3. Screen: Medical Restrictions CRUD Panel (`RestrictionsManagementFragment` / `fragment_restrictions_management.xml`, hosted in `RestrictionsActivity`)
Comprehensive database utility to manage clinical deferral types.
- **Top Input Form Panel:** Outlined `TextInputLayout` parameters for custom title inputs (`@+id/editTextNewRestrictionName`) and explicit deferral duration variables (`@+id/editTextVedaDaysCount`). Includes confirmation triggers: `@+id/buttonSaveRestriction`.
- **Bottom Database Feed:** A vertical `RecyclerView` (`@+id/recyclerViewRestrictionsList`) tracking existing parameters. Each individual item row contains an inline context delete vector action asset (`@+id/buttonDeleteRestriction`).

#### 4. Screen: Staff User Accounts CRUD Panel (`UsersManagementFragment` / `fragment_users_management.xml`, hosted in `UsersActivity`)
System employee database controller enforcing security isolation and two-step sign-up approval rules.

- **Approval Workflow & Database Flags:** Newly registered users via `SignUpFragment` are persisted by default with `isApproved = false` and `role = "NONE"`. These accounts are blocked from system entry at the login gate until modified by a Director.
- **UI Architecture:** Implement a `TabLayout` sorting accounts into two operational states:
  - **Tab 1: Pending Requests:** The `TabLayout` (`@+id/tabLayoutUsers`) sorts accounts into two `TabItem`s (pending / active). Displays a vertical `RecyclerView` for unapproved staff. Each row item card (`item_pending_user.xml`) exposes the role selection through a `MaterialButton` (`@+id/buttonPendingUserRole`, opens the role dialog with options "Administrativa" / "Directora") paired with a high-visibility activation filled button: `@+id/buttonApproveUser` (Displays: "APROBAR").
  - **Tab 2: Active Staff:** Tracks currently authorized system operators. Allows role updates and account revocations via the inline context delete vector action asset (`@+id/buttonDeleteUser`).
- **CRITICAL SECURITY MUTEX ENFORCEMENT:** The mutex is enforced **server-side**: `backend/users_admin.php` counts the active `DIRECTOR`/`DIRECTORA` accounts and returns 403 when `directorCount <= 1` (for `update_role` and `delete`). The Android client (`@+id/buttonDeleteUser`) just calls the endpoint and surfaces the backend error.

#### 5. Screen: Recovery Policies Panel (`RecoveryPoliciesFragment` / `fragment_recovery_policies.xml`, hosted in `RecoveryPoliciesActivity`)
CRUD utility to manage per-procedure recovery rules (deferral days between donations and annual caps). Seeded from `donation_system.sql` with an INCUCAI/SAS-inspired baseline (one policy per procedure: PLASMA / PLATELETS / BLOOD).
- **Top Form Panel:** procedure selector (`@+id/spinnerRecoveryProcedure`, AutoCompleteTextView with the 3 procedure codes), deferral days per gender (`@+id/editTextRecoveryDays` / `@+id/editTextRecoveryDaysFemale`) and annual caps per gender (`@+id/editTextMaxPerYear` / `@+id/editTextMaxPerYearFemale`). Save/cancel triggers: `@+id/buttonSavePolicy` / `@+id/buttonCancelEditPolicy` (cancel only visible while editing; editing keeps the row in place via `@+id/textViewRecoveryPolicyFormTitle` state).
- **Bottom Database Feed:** a vertical `RecyclerView` (`@+id/recyclerViewRecoveryPolicies`) listing existing policies with inline edit and delete actions.
- **Consumers of these policies:** `DonationsFragment` shows **non-blocking** warnings when the donor's `next_eligible_date` is still in the future or the annual donation count has reached/exceeded the policy cap (operator may continue anyway); the annual caps also feed the donor's `annualCounts` context. Backend: `recovery_policies.php`.

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
  - `is_email_reminder_sent` (TINYINT(1) DEFAULT 0) — State persistence flag for Email reminder tracking.
  - `reminder_sent_at` (DATETIME NULL) — Audit timestamp when reminder was dispatched.
  - `reminder_sent_by_user_id` (INT NULL, FOREIGN KEY referencing `users(id)` ON DELETE SET NULL) — ID of the operational staff member who dispatched the reminder.

---

### 3. Donor Vitals & Donation Snapshot (Weight / Height)
- **`donors` Table Extra Columns:**
  - `weight` (DECIMAL(5,2) NULL) — Donor's weight in kg, entered through the donor form critical data section.
  - `height` (DECIMAL(5,2) NULL) — Donor's height in cm, entered through the donor form critical data section.
- **`donations` Table Columns:**
  - `weight` (DECIMAL(5,2) NOT NULL DEFAULT 0) — Snapshot of the donor's weight captured at donation-save time.
  - `height` (DECIMAL(5,2) NOT NULL DEFAULT 0) — Snapshot of the donor's height captured at donation-save time.
  - If the donor lacks weight/height, the snapshot stores `0.0` (optional, non-blocking).

### 4. Relational Entity Cardinality Map

| Relationship | Type | Parent Table | Child / Junction Table | Purpose & Description |
| :--- | :--- | :--- | :--- | :--- |
| **Donor Contacts** | `1-to-N` | `donors` | `donor_phones`, `donor_emails` | Donors can possess multiple phone numbers and email addresses. |
| **Staff Contacts** | `1-to-N` | `users` | `user_phones`, `user_emails` | Operational staff can possess multiple contact phone numbers and emails. |
| **Donor History** | `1-to-N` | `donors` | `donations` | A donor can perform multiple donation sessions over time. |
| **Donation Extraction Types** | `1-to-N` | `donations` | `donation_procedures` | A single donation session can include concurrent apheresis extractions (e.g., Plasma + Platelets). |
| **Donor Medical Deferrals** | `N-to-M` | `donors` ↔ `restrictions` | `donor_restrictions` | Donors can have multiple active/past deferrals; deferrals apply to many donors. Stores `start_date`, `calculated_end_date`, `eligible_workday_return_date`, and free-text `notes`. |
| **Donor Appointments** | `1-to-N` | `donors` | `appointments` | A donor can schedule multiple future appointments across calendar weeks. |
| **Staff Clinical Audit** | `1-to-N` | `users` | `donations` (`staff_user_id`), `donor_restrictions` (`created_by_user_id`) | Tracks which staff member processed a donation or logged a medical deferral. |
| **Director User Approvals** | `1-to-N` | `users` | `users` (`approved_by_user_id`) | Tracks which Director approved a pending staff account and assigned its operational role. |

