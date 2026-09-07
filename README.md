# Donation System (Plasma & Platelets Bank)

A professional Android application developed for managing plasma and platelet donations. This project uses a full-stack architecture with a native Android frontend and a PHP/MySQL backend.

## 🚀 Project Overview
This system is designed to streamline the donation process for medical facilities, ensuring secure user management and accurate tracking of medical data.

### Tech Stack
- **Frontend:** Android Native (Java)
- **Backend:** PHP 8.x (Hosted via XAMPP for local development)
- **Database:** MySQL
- **Networking:** Retrofit 2 + Gson
- **Architecture:** MVC (Model-View-Controller) + Repository Pattern

## 🛠️ Setup Instructions

### Backend (XAMPP)
1. Ensure XAMPP is installed (Apache & MySQL).
2. Create a database named `donation_system`.
3. Import the latest SQL dump from the `/sql` directory.
4. **Symbolic Link:** For efficient development, create a symbolic link from your `htdocs` folder to the `backend/` folder in this repository:
   ```cmd
   mklink /D "C:\xampp\htdocs\donation-system-api" "C:\path\to\your\DonationSystem\backend"
   ```

### Android
1. Open the project in Android Studio.
2. Ensure you have Internet permissions enabled in `AndroidManifest.xml`.
3. The app is configured to talk to the local server at `10.0.2.2`.

## 🔒 Security Features
- **BCRYPT Hashing:** User passwords are encrypted using industry-standard BCRYPT before storage.
- **SQL Injection Prevention:** Backend utilizes PDO with prepared statements.
- **Network Security Config:** Secure handling of cleartext traffic for local development.

## 📂 Project Structure
- `/app`: Android source code.
- `/backend`: PHP API scripts.
- `/sql`: Database snapshots.
- `logbook.txt`: Detailed development history.
