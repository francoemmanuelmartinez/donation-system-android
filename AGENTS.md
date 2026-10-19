# AGENTS.md — DonationSystem

## Dispositivos (referencia canónica de rutas)
El proyecto se trabaja en **dos máquinas**. Esta tabla es la fuente de verdad de las rutas
(la referencian `INSTALACION.md` y `TESTING.md`). Ajustá los comandos a la máquina en la que estés.

| Elemento | Notebook (`PARDO-NET`) | PC de escritorio |
|---|---|---|
| Rol / IP LAN | Notebook de desarrollo · `192.168.0.130` (Wi-Fi) | Servidor de desarrollo · `192.168.0.202` |
| Usuario Windows | `games` | `games` |
| Repo (raíz) | `C:\Users\games\StudioProjects\donation-system-android` | `C:\Users\games\AndroidStudioProjects\DonationSystem` |
| XAMPP | `C:\xampp` | `G:\apps\xampp` |
| PHP CLI | `C:\xampp\php\php.exe` | `G:\apps\xampp\php\php.exe` |
| MySQL CLI / dump | `C:\xampp\mysql\bin\` | `G:\apps\xampp\mysql\bin\` |
| `htdocs` | `C:\xampp\htdocs` | `G:\apps\xampp\htdocs` |
| Backend publicado (junction) | `C:\xampp\htdocs\donation-system-api` → `<repo>\backend` | `G:\apps\xampp\htdocs\donation-system-api` → `<repo>\backend` |
| Base URL app (device físico) | `http://192.168.0.130/donation-system-api/` | `http://192.168.0.202/donation-system-api/` |
| Recordatorios OpenWA | Servicio Windows `OpenWA-Gateway` (NSSM, `C:\Users\games\openwa`) | Servicio Windows `OpenWA-Gateway` (NSSM, `C:\Users\games\openwa`) |

En lo que sigue, `<XAMPP>` = `C:\xampp` (notebook) o `G:\apps\xampp` (escritorio), y `<repo>` = la
raíz del repo según la tabla.

- **Backend:** PHP 8.x servido por Apache; `db_config.php` conecta a `localhost:3306`, DB
  `donation_system`, usuario `root` **sin** contraseña, `PDO::ATTR_TIMEOUT => 5`.
- **Base URL del emulador (ambas):** `http://10.0.2.2/donation-system-api/`.
- **IP hardcodeada:** `RetrofitClient.java` (`DEFAULT_BASE_URL`) y `res/xml/network_security_config.xml`
  (host en cleartext) traen la IP de una máquina. Al cambiar de dispositivo hay que actualizar **ambos**
  con la IP de la máquina que sirve el backend.

## Scripts
- `scripts\sync-db.ps1` — sincroniza la BD real de XAMPP con los SQL del repo:
  `-Push` (default) recrea `donation_system` desde `sql\donation_system.sql`; `-Seed` carga
  `sql\seed_40_donantes_turnos.sql`; `-Pull` vuelca la BD real al snapshot. Params `-Database` /
  `-MysqlBin`. Guarda: no toca `donation_system_test` sin `-Force` (ver `TESTING.md`).
- `scripts\start-xampp-mysql.ps1` — libera el puerto 3306 (detiene MySQL97) y arranca MariaDB de
  XAMPP. `-Restore` lo revierte. Requiere administrador (UAC).
- `scripts\check-encoding.ps1` — verificación (solo lectura) de codificación: BOM UTF-8, `U+FFFD`,
  mojibake (bytes `C3 83`, `E2 82 AC`, etc.) y la corrupción histórica `í→á` (patrones `á`+`a`,
  `á`+`os`, etc.). Sale con código ≠ 0 si hay hallazgos. Params `-Path`, `-IncludeUntracked`, `-Quiet`.

## OpenWA (recordatorios automáticos por WhatsApp)
Gateway open-source (Node + Baileys, **sin Chromium**) en `C:\Users\games\openwa`; corre como
servicio Windows **`OpenWA-Gateway`** (NSSM, auto-start/restart). Sirve API + dashboard en
`http://localhost:2785`. Solo lo consume el backend (`send_reminders.php`); la app Android no lo usa.
- **Instalado (notebook, 14-oct-2026):** Node 24 LTS, Python 3.12, NSSM (winget); `.env` con
  `ENGINE_TYPE=baileys`, `DATABASE_TYPE=sqlite`, `AUTO_START_SESSIONS=true`, `SERVE_DASHBOARD=true`;
  `npm ci` con `PUPPETEER_SKIP_DOWNLOAD=1` (evita bajar Chromium) + `npm run build:all`.
- **Sesión:** `donacion`, chip `5492233556122`. Credenciales en
  `backend/config_reminders_override.php` (gitignored). Re-vincular = escanear QR (único paso manual).
- **Scheduler:** tarea `RecordatoriosDonacion`, diaria 08:00 (`backend/install_scheduler.php`).
  *(En la notebook fue **eliminada** y el lazy sweep quedó **desactivado**; el único disparo es el
  botón de la Agenda — ver "Nota (recordatorios)" más abajo.)*
- **Scripts:** `openwa\openwa-dashboard.bat` / `openwa\start-openwa.bat` *(obsoleto para el
  gateway)* / `openwa\stop-openwa.bat` *(solo emergencias)*.
- **Estado:** `Get-Service OpenWA-Gateway` · `curl http://localhost:2785/api/health`.
- Guía completa: `whatsapp-reminders-guide.md` §7.

## Problemas conocidos y fixes
### Puerto 3306 ocupado por MySQL97 (notebook)
- **Síntoma:** la app da 500 `Access denied for user 'root'@'localhost'` o "no conecta".
- **Causa:** el servicio Windows `MySQL97` (MySQL Server 9.7, aparte de XAMPP) toma el 3306 y
  exige contraseña de root; MariaDB de XAMPP no puede bindear (en `mysql_error.log`:
  *"Do you already have another mysqld server running on port: 3306"*) y **aborta**.
- **Fix:** `scripts\start-xampp-mysql.ps1` (admin): detiene `MySQL97`, lo deja en **Manual** y
  arranca MariaDB de XAMPP. Revertir con `-Restore`. Verificar: `mysqladmin -u root ping` →
  *"mysqld is alive"*.
- **Ojo:** `mysql_stop.bat` mata **todos** los `mysqld.exe` (incluido MySQL97); usar
  `mysqladmin -u root shutdown` o el script para detener solo MariaDB.

### MySQL/MariaDB no arranca: `multi-master.info` corrupto
- **Síntoma:** la app "no loguea / timeout"; `mysqladmin ping` se cuelga; el panel `xampp-control.exe` crashea (APPCRASH `c0000005`).
- **Causa:** el archivo `<XAMPP>\mysql\data\multi-master.info` quedó con **líneas de log** en vez de nombres de conexiones
  (típicamente tras un apagado sucio / BSOD). mysqld lo interpreta como conexiones de replicación, crea archivos basura
  (`master-*`, `mysql-relay-bin-*`, `relay-log-*`) y **muere antes de "ready for connections"** (el log se corta en `Server socket created`).
- **Fix:**
  1. Asegurar mysqld detenido (`mysql_stop.bat` o `taskkill /F /IM mysqld.exe`).
  2. Mover a respaldo **fuera del datadir** (no borrar): `multi-master.info` + todos los `master-*`, `mysql-relay-bin-*`, `relay-log-*`.
     Ej.: `<XAMPP>\mysql\_repl_backup_<fecha>\`.
  3. Arrancar MySQL (`mysql_start.bat`). Verificar: `mysqladmin -u root ping` → *"mysqld is alive"* y en el log aparece *"ready for connections"*.
- **Prevención:** apagar XAMPP por el panel / `mysql_stop.bat`; evitar cortes de energía forzados.
- **Ojo:** no crear carpetas dentro de `mysql\data` (MySQL las lista como bases de datos).

### MariaDB no arranca: redo log de InnoDB corrupto (`Missing MLOG_CHECKPOINT`)
- **Síntoma:** el panel XAMPP dice *"MySQL shutdown unexpectedly"*; `mysqld` **aborta al arrancar**.
- **Causa:** el redo log de InnoDB quedó inconsistente (apagado sucio / kill `taskkill /F` mientras
  MariaDB corría). En `<XAMPP>\mysql\data\mysql_error.log` aparece:
  *"InnoDB: Missing MLOG_CHECKPOINT at N between the checkpoint N and the end M"* y luego
  *"Unknown/unsupported storage engine: InnoDB"* → aborta. **No** es `multi-master.info` ni puerto 3306.
- **Fix (rebuild limpio — la instancia vieja NO se recupera sola):**
  1. Resguardo frío: detener mysqld y copiar `<XAMPP>\mysql\data` completo + `my.ini` a
     `<XAMPP>\mysql\_corrupt_<fecha>\` (por si hace falta).
  2. Opcional: `mysqldump -u root --databases donation_system > _corrupt_<fecha>\donation_system.sql`
     (sirve si la instancia aún arranca; los datos son reproducibles desde `sql\`).
  3. Detener mysqld (`mysqladmin -u root shutdown`). Mover `<XAMPP>\mysql\data` a
     `_corrupt_<fecha>\data_used`.
  4. Generar un datadir **desde cero**: `mysql_install_db.exe --datadir="<XAMPP>\mysql\data"`
     (crea las tablas del sistema y el redo log con LSN consistente; root queda sin contraseña).
  5. Arrancar mysqld y verificar `mysqladmin -u root ping` → *"mysqld is alive"*.
  6. Reconstruir la BD: `scripts\sync-db.ps1 -Push -Seed`.
- **Ojo:** borrar solo los `ib_logfile*` deja que MariaDB arranque, pero `ibdata1` queda con un
  checkpoint atrás y en cada arranque aparecen *"Page [...] log sequence number ... is in the
  future! Current system log sequence number ..."* (persistente). Preferir el rebuild con
  `mysql_install_db.exe`.
- **Prevención:** `innodb_log_file_size=128M` en `my.ini` (era `5M`); apagar por el panel /
  `mysqladmin -u root shutdown`; **no** usar `mysql_stop.bat` ni `taskkill /F /IM mysqld.exe`;
  cuidar cortes de energía.

## Convenciones de archivos (encoding y saltos de línea)
- **UTF-8 sin BOM** en todo archivo de texto. Un BOM (`EF BB BF`) antes de `<?php` rompe el JSON o
  el navegador (ya nos pasó). Verificá con `scripts\check-encoding.ps1`.
- **No reescribir archivos completos** para "arreglar" acentos o mojibake: hacé **ediciones
  puntuales** sobre frases exactas, nunca volcar el archivo entero (así se re-copió la corrupción).
- **Una sola guía por tema.** No generar variantes (`*_v2.md`, `*_fix.md`, etc.). La guía de los
  recordatorios del proyecto es `whatsapp-reminders-guide.md`; la **demo** (archivos genéricos
  copiables) vive en `openwa-demo/`, con su tutorial `openwa-demo/GUIA-OPENWA-INTEGRACION.md`.
- **Ojo con `í`:** hubo una corrupción histórica que reemplazó `í` por `á` (patrones `á`+`a`, `á`+`os`,
  `á`+`as`, `á`+`o`). Si aparecen donde debería ir `ía`/`íos`/`ías`/`ío`, revisá con el script antes
  de commitear.
- **No commitear ruido de fin de línea:** el repo mezcla CRLF/LF por `core.autocrlf=true`; commiteá
  solo lo que tenga cambio real (`git diff --stat`).

## Notas
- Directorio de trabajo en español; textos UI con tildes permitidas.
- Ver `TESTING.md` para el enfoque de pruebas.
- Los ajustes recientes (base URL `10.0.2.2`, cleartext `192.168.0.130`, cuenta `admin`, scripts de
  sync y BD) son para dejar operativa la app en la **notebook**; la PC de escritorio ya funciona.
- La **notebook ya tiene OpenWA instalado** (servicio `OpenWA-Gateway`); ver sección "OpenWA".
  Sin scheduler ni lazy sweep: los recordatorios se disparan **solo desde el botón** de la Agenda.

> **Nota (recordatorios, notebook PARDO-NET — cambio exclusivo de este dispositivo):** en la
> notebook se **eliminó** la tarea programada `RecordatoriosDonacion` (`schtasks /Delete /TN
> "RecordatoriosDonacion" /F`) y se **desactivó el lazy sweep** vía
> `REMINDER_LAZY_SWEEP_ENABLED = false` en `backend/config_reminders_override.php` (gitignored, no
> afecta a la PC de escritorio). Único disparo: el **botón de la Agenda**
> (`action=dispatch_reminders`, `manual=true`). El envío sigue siendo **pausado/secuencial** (mismos
> `MANUAL_DELAY_*` / `BATCH_SPREAD_MIN_SECONDS` que el escritorio). Para reactivar el automático:
> `php backend/install_scheduler.php` y poner el flag en `true`.

> **Nota (notebook PARDO-NET):** los ajustes de este repo —base URL al emulador (`10.0.2.2`), host
> `192.168.0.130` en el cleartext, cuenta `admin`, scripts de sync y BD— están orientados a dejar
> operativa la app en la **notebook**. La **PC de escritorio** ya funciona con su configuración
> actual y no requiere estos cambios.
