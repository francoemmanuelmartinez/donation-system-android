# AGENTS.md — DonationSystem

## Entorno (esta máquina)
- Backend PHP en `C:\Users\games\AndroidStudioProjects\DonationSystem\backend`.
  Se sirve por Apache como `http://192.168.0.202/donation-system-api/` (esta PC **es** 192.168.0.202).
- `G:\apps\xampp\htdocs\donation-system-api` es un **junction** → `...\DonationSystem\backend`:
  editar el repo = editar el backend live (no hay copia separada).
- XAMPP en `G:\apps\xampp` (Apache 2.4.58 / PHP 8.2.12 / MariaDB 10.4.32).
  - MySQL: `G:\apps\xampp\mysql\bin\mysqld.exe`, arranque `mysql_start.bat` / parada `mysql_stop.bat`.
  - DB: `donation_system`, usuario `root`, sin contraseña.
  - Log de error: `G:\apps\xampp\mysql\data\mysql_error.log`.
  - Base URL en la app: `http://192.168.0.202/donation-system-api/` (`RetrofitClient.java`).
- Recordatorios WhatsApp vía **OpenWA**, gateway como **servicio Windows `OpenWA-Gateway`** (NSSM, `C:\Users\games\openwa`).
  Dashboard manual: `openwa-dashboard.bat`. Ver `whatsapp-reminders-guide.md`.
- `db_config.php` usa `PDO::ATTR_TIMEOUT => 5` para que MySQL colgado falle rápido (500) en vez de colgar la request.

## Problemas conocidos y fixes
### MySQL/MariaDB no arranca: `multi-master.info` corrupto
- **Síntoma:** la app "no loguea / timeout"; `mysqladmin ping` se cuelga; el panel `xampp-control.exe` crashea (APPCRASH `c0000005`).
- **Causa:** el archivo `G:\apps\xampp\mysql\data\multi-master.info` quedó con **líneas de log** en vez de nombres de conexiones
  (típicamente tras un apagado sucio / BSOD). mysqld lo interpreta como conexiones de replicación, crea archivos basura
  (`master-*`, `mysql-relay-bin-*`, `relay-log-*`) y **muere antes de "ready for connections"** (el log se corta en `Server socket created`).
- **Fix:**
  1. Asegurar mysqld detenido (`mysql_stop.bat` o `taskkill /F /IM mysqld.exe`).
  2. Mover a respaldo **fuera del datadir** (no borrar): `multi-master.info` + todos los `master-*`, `mysql-relay-bin-*`, `relay-log-*`.
     Ej.: `G:\apps\xampp\mysql\_repl_backup_<fecha>\`.
  3. Arrancar MySQL (`mysql_start.bat`). Verificar: `mysqladmin -u root ping` → *"mysqld is alive"* y en el log aparece *"ready for connections"*.
- **Prevención:** apagar XAMPP por el panel / `mysql_stop.bat`; evitar cortes de energía forzados.
- **Ojo:** no crear carpetas dentro de `mysql\data` (MySQL las lista como bases de datos).
- Servicio `MySQL97` (MySQL 9.7 aparte) dejado en **Manual** para no pelear por el puerto 3306.

## Notas
- Directorio de trabajo en español; textos UI con tildes permitidas.
- Ver `TESTING.md` para el enfoque de pruebas.
