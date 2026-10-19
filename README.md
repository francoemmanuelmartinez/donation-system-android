# Donation System (Plasma & Platelets Bank)

A professional Android application developed for managing plasma and platelet donations. This project uses a full-stack architecture with a native Android frontend and a PHP/MySQL backend.

## 🚀 Project Overview
This system is designed to streamline the donation process for medical facilities, ensuring secure user management and accurate tracking of medical data.

### Tech Stack
- **Frontend:** Android Native (Java)
- **Backend:** PHP 8.x (Hosted via XAMPP for local development)
- **Database:** MySQL
- **Networking:** Retrofit 2 + Gson
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern

## 🛠️ Setup Instructions

> **Máquina nueva (notebook):** seguí la guía paso a paso en **`INSTALACION.md`**
> (XAMPP, import de BD, junction, configuración de red de la app).

### Backend (XAMPP)
1. Ensure XAMPP is installed (Apache & MySQL).
2. Create a database named `donation_system`.
3. Import `sql/donation_system.sql` (self-contained: schema + `reminder_dispatch_control`
   + search indexes + seed). Optionally import `sql/seed_40_donantes_turnos.sql` for demo data.
4. **Junction:** publish the `backend/` folder inside `htdocs` without copying:
   ```cmd
   mklink /J "C:\xampp\htdocs\donation-system-api" "C:\path\to\your\DonationSystem\backend"
   ```
   (`/J` = directory junction, no admin rights needed; a plain symlink `/D` would require them.)

### Android
1. Open the project in Android Studio.
2. Ensure you have Internet permissions enabled in `AndroidManifest.xml`.
3. Set the backend base URL in `RetrofitClient.java` (`DEFAULT_BASE_URL`), and whitelist that
   host in `res/xml/network_security_config.xml` for cleartext HTTP. Common values:
   - Emulator: `http://10.0.2.2/donation-system-api/`
   - Physical device on LAN: `http://<your-PC-IP>/donation-system-api/`


## 🔒 Security Features
- **BCRYPT Hashing:** User passwords are encrypted using industry-standard BCRYPT before storage.
- **SQL Injection Prevention:** Backend utilizes PDO with prepared statements.
- **Network Security Config:** Secure handling of cleartext traffic for local development.

## ⚠️ Known Limitations (current state)
- **The API has no authentication/authorization:** no endpoint validates a session or role; access control is enforced only in the Android client (`LoginFragment.routeByRole`). Suitable for a trusted LAN only — do not expose the backend publicly.
- **No logout / session restore:** the app always starts at the login screen; a persisted session is only used to attribute actions (`created_by`).
- **Network logging:** Retrofit logs full request/response bodies (`Level.BODY`) in **all** builds, including release.

## 📂 Project Structure
- `/app`: Android source code.
- `/backend`: PHP API scripts.
- `/sql`: Database snapshots (`donation_system.sql` is the self-contained dump; the `migration_*.sql` files are historical steps already baked into it — no need to run them on a fresh install).
- `logbook.txt`: Detailed development history (frozen as of 2026-09-23; the live history is `git log`).
