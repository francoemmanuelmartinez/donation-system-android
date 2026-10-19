# Technical Summary: Donation System Architecture

**Version:** 1.0  
**Project Lead:** Franco Martinez  
**Context:** Plasma and Platelets Bank Application

---

## 1. Architectural Patterns
The project follows a "Clean Architecture" approach using Android MVVM:

### Frontend (Android)
- **MVVM (Model-View-ViewModel):** Presentation logic is decoupled from the UI using Jetpack `ViewModel` and `LiveData`. Activities (`views`) observe UI state encapsulated in `Resource<T>` objects, while ViewModels manage state transitions and delegate data requests to the repository layer.
- **Repository Pattern:** A middle layer between ViewModels and data sources. This allows the app to be data-source agnostic (e.g., swapping from a local DB to a remote API without changing UI or ViewModel code).
- **Retrofit Client:** Centralized networking configuration using a Singleton pattern to manage API connections efficiently.

### Backend (PHP API)
- **Monorepo Strategy:** The API source code is stored within the same Git repository as the Android code, ensuring version consistency across the entire stack.
- **Stateless REST-like API:** Communication is handled via JSON over HTTP (GET and POST endpoints).

---

## 2. Project Layout & Package Responsibilities

The monorepo architecture organizes frontend, backend, and database components as follows:

### Root Directory Overview
- **`/app`**: Native Android application source code, resources, and Gradle build configuration.
- **`/backend`**: PHP 8.x REST API scripts served locally via XAMPP using a Windows directory junction (`mklink /J`, `htdocs`).
- **`/sql`**: Database schema dumps (`donation_system.sql`) for MySQL / phpMyAdmin database setup.
- **`/gradle`**: Gradle Wrapper executables ensuring consistent Gradle version builds.
- **`/.idea`**: Android Studio project metadata and workspace settings.

### Android Package Structure (`app/src/main/java/com/francomartinez/donationsystem`)
- **`views/`**: Activity/Fragment UI screens (`AuthActivity` with `LoginFragment`/`SignUpFragment`, `MainActivity`, `DirectorDashboardActivity`) that observe ViewModel state.
- **`viewmodels/`**: Jetpack ViewModels (`LoginViewModel`, `RegisterViewModel`) handling UI business logic and state.
- **`repositories/`**: Repository layer (`UserRepository`) abstracting network and local data sources.
- **`models/`**: Data transfer objects and domain models (`User`, `LoginResponse`, `Donor`).
- **`api/`**: Networking configuration (`ApiService`, `RetrofitClient`).
- **`utils/`**: Utilities and state wrappers like `Resource<T>` (`LOADING`, `SUCCESS`, `ERROR`).
- **`database/`**: Placeholder package reserved for a future Room integration (`PlaceholderDao`). **Not active today** — Room is not a dependency; the app is online-only against the PHP API.

---

## 3. Security Implementation
Security was prioritized from Day 1 to meet medical data standards:

- **Password Security:** We do not store plain-text passwords. The backend uses `password_hash()` with `PASSWORD_DEFAULT` (BCRYPT), ensuring that even a database breach does not compromise user credentials.
- **Database Access:** The PHP layer uses **PDO (PHP Data Objects)**. We strictly use **Prepared Statements** to eliminate the risk of SQL Injection attacks.
- **Android Network Security:** A custom `network_security_config.xml` is implemented to securely whitelist the local development environment (`10.0.2.2`) while maintaining high security for production domains.

---

## 4. Synchronization & Workflow
To support development across multiple devices (e.g., PC and Notebook):
- **Database Dumps:** The self-contained SQL dump lives in `/sql` (`donation_system.sql` includes schema, the `reminder_dispatch_control` table and the search indexes). Demo data is optional (`seed_40_donantes_turnos.sql`).
- **Junction:** The `backend/` folder is exposed to XAMPP via a Windows **directory junction** (`mklink /J "<xampp>\htdocs\donation-system-api" "<repo>\backend"`), so the web server serves the IDE-managed files without manual copying. Full fresh-machine steps are in `INSTALACION.md`.

---

## 5. Future Roadmap
- Implementation of **Medical Record Tracking** (Donation types, Blood Pressure, Volume).
- Integration of **Room Database** for offline-first capabilities.
- Development of a **C++ Desktop Application** sharing the same central PHP/MySQL backend.
