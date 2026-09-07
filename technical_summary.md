# Technical Summary: Donation System Architecture

**Version:** 1.0  
**Project Lead:** Franco Martinez  
**Context:** Plasma and Platelets Bank Application

---

## 1. Architectural Patterns
The project follows a "Clean Architecture" approach adapted for Android MVC:

### Frontend (Android)
- **MVC (Model-View-Controller):** Logic is decoupled from the UI. Activities (`views`) handle interactions, while `controllers` orchestrate business logic.
- **Repository Pattern:** A middle layer between controllers and data sources. This allows the app to be data-source agnostic (e.g., swapping from a local DB to a remote API without changing UI code).
- **Retrofit Client:** Centralized networking configuration using a Singleton pattern to manage API connections efficiently.

### Backend (PHP API)
- **Monorepo Strategy:** The API source code is stored within the same Git repository as the Android code, ensuring version consistency across the entire stack.
- **Stateless REST-like API:** Communication is handled via JSON over HTTP POST requests.

---

## 2. Security Implementation
Security was prioritized from Day 1 to meet medical data standards:

- **Password Security:** We do not store plain-text passwords. The backend uses `password_hash()` with `PASSWORD_DEFAULT` (BCRYPT), ensuring that even a database breach does not compromise user credentials.
- **Database Access:** The PHP layer uses **PDO (PHP Data Objects)**. We strictly use **Prepared Statements** to eliminate the risk of SQL Injection attacks.
- **Android Network Security:** A custom `network_security_config.xml` is implemented to securely whitelist the local development environment (`10.0.2.2`) while maintaining high security for production domains.

---

## 3. Synchronization & Workflow
To support development across multiple devices (e.g., PC and Notebook):
- **Database Dumps:** Regular SQL exports are stored in `/sql`.
- **System Links:** Using OS-level symbolic links, we allow the IDE to manage files directly while the local web server (XAMPP) serves them without manual copying.

---

## 4. Future Roadmap
- Implementation of **Medical Record Tracking** (Donation types, Blood Pressure, Volume).
- Integration of **Room Database** for offline-first capabilities.
- Development of a **C++ Desktop Application** sharing the same central PHP/MySQL backend.
