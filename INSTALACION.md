# Instalación en máquina nueva

Guía para clonar y dejar funcionando **DonationSystem** (app Android + backend PHP/MySQL)
en una PC nueva (por ejemplo, una notebook) desde cero.

Este documento cubre: XAMPP, importación de la base de datos, el **junction** que publica
el backend en `htdocs`, la configuración de red de la app Android y una nota sobre OpenWA.
La app **funciona sin OpenWA** (OpenWA solo habilita el recordatorio automático por WhatsApp).

> **Nota (notebook PARDO-NET):** los ajustes de esta guía —base URL al emulador (`10.0.2.2`), host `192.168.0.130` en el cleartext, cuenta `admin`, scripts de sync y BD— están orientados a dejar operativa la app en la **notebook**. La **PC de escritorio** ya funciona con su configuración actual y no requiere estos cambios.

> **Rutas por dispositivo:** la fuente canónica es la tabla **"Dispositivos"** de `AGENTS.md`.
> Ahí están XAMPP, el repo, `htdocs` e IP de cada máquina: notebook `PARDO-NET` (`192.168.0.130`,
> XAMPP en `C:\xampp`) y PC de escritorio (`192.168.0.202`, XAMPP en `G:\apps\xampp`).
> Donde este documento use `C:\xampp`, reemplazalo por la ruta real de tu XAMPP.

---

## 1. Requisitos previos

| Herramienta | Notas |
|---|---|
| **Android Studio** (con SDK + emulador) | Para compilar/correr la app. |
| **XAMPP** (Apache + MySQL/MariaDB) | Backend PHP + base de datos. |
| **Git** | Clonar el repo. |
| **JDK 17** (o el requerido por el proyecto) | Normalmente lo trae Android Studio. |

Opcional (solo para recordatorios automáticos por WhatsApp): **Node.js ≥ 22.19** + OpenWA
(ver sección 7).

---

## 2. Clonar el repositorio

```powershell
git clone <URL-del-repo> C:\Users\<usuario>\AndroidStudioProjects\DonationSystem
```

Todos los comandos siguientes asumen que el repo quedó en
`C:\Users\<usuario>\AndroidStudioProjects\DonationSystem`; ajustá si es otra ruta.

---

## 3. XAMPP (Apache + MySQL)

1. Abrí el **XAMPP Control Panel** y arrancá **Apache** y **MySQL**.
2. Verificá MySQL:

   ```powershell
   & "C:\xampp\mysql\bin\mysqladmin.exe" -u root ping
   # -> mysqld is alive
   ```

3. Verificá Apache: `http://localhost/` debe responder.

> Puertos por defecto: Apache 80, MySQL 3306. Si algo los ocupa, cambialos en el panel.
> Usuario MySQL: `root`, **sin contraseña** (config por defecto del proyecto).

---

## 4. Base de datos

### 4.1 Crear la base

```powershell
& "C:\xampp\mysql\bin\mysql.exe" -u root -e "CREATE DATABASE IF NOT EXISTS donation_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 4.2 Importar el esquema (autosuficiente)

`sql/donation_system.sql` **ya incluye** todo el esquema: tablas, la tabla
`reminder_dispatch_control`, los índices del módulo de búsqueda, el diccionario de
restricciones, las políticas de recuperación y dos cuentas seed (Directora y `admin`).

```powershell
cmd /c '"C:\xampp\mysql\bin\mysql.exe" -u root donation_system < "C:\Users\<usuario>\AndroidStudioProjects\DonationSystem\sql\donation_system.sql"'
```

> PowerShell no soporta el redirect `<`; por eso se usa `cmd /c "..."` con comillas simples.
> Alternativa: `Get-Content ...\donation_system.sql | & "C:\xampp\mysql\bin\mysql.exe" -u root donation_system`.

### 4.3 Datos de demo (opcional)

Para tener donantes y turnos de prueba (40 donantes + turnos de octubre 2026, con
teléfonos/emails, donaciones previas y restricciones para generar estados EN VEDA):

```powershell
cmd /c '"C:\xampp\mysql\bin\mysql.exe" -u root donation_system < "C:\Users\<usuario>\AndroidStudioProjects\DonationSystem\sql\seed_40_donantes_turnos.sql"'
```

> Es de **un solo uso**: los DNI son `UNIQUE`. Si lo reimportás, fallará por duplicados.
> No borra datos: crea donantes nuevos (ids 3..42).

### 4.4 Verificar

```powershell
& "C:\xampp\mysql\bin\mysql.exe" -u root -e "USE donation_system; SHOW TABLES; SELECT COUNT(*) AS donors FROM donors;"
```

Deberías ver 13 tablas (incluida `reminder_dispatch_control`).

### 4.5 Credenciales seed

| Uso | Usuario | Contraseña |
|---|---|---|
| Cuenta Directora (import de `donation_system.sql`) | `director@clinic.org` | `password` |
| Cuenta demo (import de `donation_system.sql`) | `admin` | `admin` |
| Tests automatizados (solo existen en `donation_system_test`) | `director@test.local` | `password` |

> En `donation_system` (datos reales) la cuenta de test **no existe**. Si la necesitás,
> creá una Directora desde la pantalla de registro de la app o insertala en MySQL (hash BCrypt).

### 4.6 Sincronización con el repo (script)

`scripts\sync-db.ps1` empaqueta los pasos 4.1–4.3 y mantiene alineadas la **BD real de XAMPP**
y los **SQL del repo**:

```powershell
# Recrea donation_system desde sql\donation_system.sql y carga el seed de demo
powershell -ExecutionPolicy Bypass -File .\scripts\sync-db.ps1 -Push -Seed

# Volcar la BD real al snapshot versionado (mysqldump -> sql\donation_system.sql)
powershell -ExecutionPolicy Bypass -File .\scripts\sync-db.ps1 -Pull
```

- `-Push` (default): `DROP`+`CREATE` de la base e importa `sql\donation_system.sql`.
- `-Seed`: importa `sql\seed_40_donantes_turnos.sql` (un solo uso; los DNI son `UNIQUE`).
- `-Pull`: vuelca la BD real al snapshot del repo.
- `-Database` / `-MysqlBin`: otra base o ruta de XAMPP.
- **Guarda:** no opera sobre `donation_system_test` sin `-Force` (ver `TESTING.md`).
- Requiere MariaDB de XAMPP en `localhost:3306`. Si el puerto lo tiene `MySQL97`,
  ver sección 8 (o correr `scripts\start-xampp-mysql.ps1`).

> En la PC de escritorio (XAMPP en `G:\apps\xampp`) pasá
> `-MysqlBin G:\apps\xampp\mysql\bin`.

---

## 5. Publicar el backend en Apache (junction)

Apache sirve los archivos desde `htdocs`. En lugar de copiar `backend/`, se crea un
**junction** (tipo de enlace de directorio de Windows) que apunta a la carpeta del repo.
Así, editar el repo = editar el backend que sirve Apache (sin copias).

> **Junction (`/J`) vs symlink (`/D`)**: el symlink de directorio requiere privilegios de
> administrador (o modo desarrollador). El **junction no** y es suficiente.

Con la carpeta destino **inexistente** aún:

```cmd
mklink /J "C:\xampp\htdocs\donation-system-api" "C:\Users\<usuario>\AndroidStudioProjects\DonationSystem\backend"
```

- Si `donation-system-api` ya existe, borrala antes (o movela).
- Ejecutá el comando en `cmd.exe` (no en PowerShell).

Verificar:

```powershell
cmd /c dir /AL "C:\xampp\htdocs"
# -> donation-system-api [C:\Users\<usuario>\...\backend]

Invoke-WebRequest "http://localhost/donation-system-api/login.php" -Method Head
# -> responde (200/400), no 404
```

### Base URL del backend

- Desde el **emulador Android**: `http://10.0.2.2/donation-system-api/` (`10.0.2.2` = tu PC).
- Desde un **dispositivo físico en la misma red**: `http://<IP-LAN-de-tu-PC>/donation-system-api/`.
- Tests de integración (en la misma PC): `http://localhost/donation-system-api/`.

---

## 6. Configuración de la app Android

### 6.1 Base URL — `RetrofitClient.java`

La URL por defecto está hardcodeada y es **específica de la máquina**:

`app/src/main/java/com/francomartinez/donationsystem/api/RetrofitClient.java`

```java
private static final String DEFAULT_BASE_URL = "http://10.0.2.2/donation-system-api/";
```

Cambiala según corresponda:

- Emulador: `http://10.0.2.2/donation-system-api/`
- Device físico en LAN: `http://<IP-de-tu-PC>/donation-system-api/`

### 6.2 Cleartext (HTTP) — `network_security_config.xml`

`app/src/main/res/xml/network_security_config.xml` **solo permite tráfico HTTP** hacia el
host listado. Si tu base URL usa otro host, agregalo aquí o Android **bloqueará la request**
(el síntoma es "no conecta con el servidor / no encuentra la BD"):

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">192.168.0.202</domain>
        <domain includeSubdomains="true">192.168.0.130</domain>
    </domain-config>
</network-security-config>
```

### 6.3 `local.properties` (autogenerado)

`local.properties` está en `.gitignore` y lo regenera Android Studio al abrir el proyecto
(define `sdk.dir`). No se versiona.

### 6.4 Compilar y correr

```powershell
.\gradlew.bat assembleDebug
```

Luego Run en Android Studio. Login con la cuenta seed (sección 4.5).

---

## 7. OpenWA (OPCIONAL — solo recordatorios automáticos)

La app **no necesita OpenWA** para funcionar: las donaciones, turnos, búsqueda, estadísticas
y el recordatorio **manual** (botón 💬 abre WhatsApp del teléfono) funcionan sin él.

OpenWA solo aporta el **envío automático** por WhatsApp (2 días antes del turno) desde el
servidor. Si no vas a usarlo:

- Dejar `REMINDER_ENABLED = false` en `backend/config_reminders.php` para evitar intentos de envío.
- Las credenciales de OpenWA (`OPENWA_API_KEY`, `OPENWA_SESSION_ID`) **no** están versionadas;
  viven en `backend/config_reminders_override.php` (ver 7.1).

Guía completa (instalación de OpenWA, sesión/QR, servicio Windows con NSSM, fallback a Meta):
**`whatsapp-reminders-guide.md`, sección 7** (7.8 instalación · 7.12 operación).

> **Nota (notebook PARDO-NET):** OpenWA ya está instalado — Node 24 LTS + gateway en
> `C:\Users\games\openwa`, servicio Windows `OpenWA-Gateway` (NSSM, auto-start) y sesión
> `donacion` (chip `5492233556122`). Estado: `Get-Service OpenWA-Gateway` ·
> `curl http://localhost:2785/api/health`. Ver `AGENTS.md` → "OpenWA".

> **Nota (recordatorios, notebook PARDO-NET — cambio exclusivo de este dispositivo):** en la
> notebook **no hay scheduler** (se eliminó la tarea `RecordatoriosDonacion`) y el **lazy sweep
> quedó desactivado** (`REMINDER_LAZY_SWEEP_ENABLED = false` en
> `backend/config_reminders_override.php`, gitignored). El **único disparo** es el **botón de la
> Agenda**. El envío sigue siendo pausado/secuencial. La PC de escritorio no se ve afectada.

### 7.1 Secretos fuera del repo

`backend/config_reminders.php` no contiene secretos. Si definís
`backend/config_reminders_override.php` (en `.gitignore`), se carga automáticamente:

```php
<?php
// backend/config_reminders_override.php  — NO versionado
define('OPENWA_BASE_URL', 'http://localhost:2785');
define('OPENWA_API_KEY', 'owa_k1_...');
define('OPENWA_SESSION_ID', '<session-id>');
```

---

## 8. Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| La app "no conecta" / timeout | Base URL incorrecta | Ajustar `RetrofitClient.DEFAULT_BASE_URL` (sección 6.1). |
| Requests HTTP bloqueadas | Host no listado en cleartext | Agregar la IP en `network_security_config.xml` (sección 6.2). |
| `http://localhost/donation-system-api/` da 404 | Junction no creado o mal | Recrear el junction (sección 5). |
| 500 `Database Connection Failed` | MySQL apagado o base ausente | Arrancar MySQL / importar `donation_system.sql`. |
| 500 `Access denied for user 'root'@'localhost'` | El servicio `MySQL97` (MySQL 9.7) ocupa el 3306 y exige contraseña | `scripts\start-xampp-mysql.ps1` (admin): detiene MySQL97, lo deja en Manual y arranca MariaDB. Ver `AGENTS.md` → "Puerto 3306 ocupado por MySQL97". |
| MariaDB aborta: *"running on port: 3306"* | Otro `mysqld` (p. ej. MySQL97) tiene el puerto | Igual que la fila anterior. |
| MySQL colgado / no arranca | `multi-master.info` corrupto (apagado sucio) | Ver `AGENTS.md` → "MySQL/MariaDB no arranca". |
| `mklink` falla | Destino ya existe | Borrar/renombrar la carpeta destino y reintentar. |
| `CREATE INDEX IF NOT EXISTS` falla | Estás en MySQL puro (no MariaDB) | Quitar `IF NOT EXISTS` del dump (XAMPP trae MariaDB, funciona). |

---

## 9. Anexo — rutas de referencia

Fuente canónica: tabla **"Dispositivos"** de `AGENTS.md`. Resumen:

| Elemento | Notebook (`PARDO-NET`, `192.168.0.130`) | PC de escritorio (`192.168.0.202`) |
|---|---|---|
| Repo | `C:\Users\games\StudioProjects\donation-system-android` | `C:\Users\games\AndroidStudioProjects\DonationSystem` |
| XAMPP | `C:\xampp` | `G:\apps\xampp` |
| PHP CLI | `C:\xampp\php\php.exe` | `G:\apps\xampp\php\php.exe` |
| MySQL CLI | `C:\xampp\mysql\bin\mysql.exe` | `G:\apps\xampp\mysql\bin\mysql.exe` |
| `htdocs` | `C:\xampp\htdocs` | `G:\apps\xampp\htdocs` |
| Backend publicado | `<htdocs>\donation-system-api` (junction → `<repo>\backend`) | idem |
| Base URL app (device) | `http://192.168.0.130/donation-system-api/` | `http://192.168.0.202/donation-system-api/` |
| Base URL emulador | `http://10.0.2.2/donation-system-api/` | idem |
