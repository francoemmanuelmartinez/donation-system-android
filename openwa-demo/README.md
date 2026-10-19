# openwa-demo — archivos listos para copiar

Los mismos archivos que explican la [guía](GUIA-OPENWA-INTEGRACION.md), pero **acá
están completos y listos para pegar en tu proyecto**. Sin fragmentos ni "armá esto
vos".

Cubre estas **funcionalidades**:

| # | Funcionalidad | Cómo se dispara |
|---|---|---|
| 1 | Enviar todos vía **OpenWA** (servidor) | Botón "① Enviar pendientes ahora (servidor/OpenWA)" — usa datos de la BD |
| 2 | Enviar por **`wa.me`** (celular) | Botón "④ Abrir WhatsApp del 1er pendiente de la BD (wa.me)" |
| 3 | **Automático** sin tocar nada | Al abrir la pantalla + cron del servidor (lazy sweep 1 vez/día) |
| 4 | Crear recordatorio en BD | Botón "② Crear recordatorio en la BD" (con delay en minutos) |
| 5 | wa.me directo (sin BD) | Botón "③ Abrir WhatsApp con lo del formulario (wa.me directo)" |
| 6 | Reiniciar gate lazy sweep | Botón "⑤ Reiniciar gate del lazy sweep (solo demo)" |

> **Pausa entre mensajes:** al enviar varios, la demo espera 20-60 s **al azar** entre uno y otro
> (*jitter*) para no parecer un robot y bajar el riesgo de que WhatsApp bloquee el número.

---

## Backend (PHP)

`backend/` → copiá los archivos a tu servidor web (XAMPP: `htdocs/tu-backend/`).
`openwa_config.php` **no viene** en el repo (tiene secretos): copiá
`openwa_config.example.php` y renombralo a `openwa_config.php`.

| Archivo | Para qué sirve |
|---|---|
| `recordatorios.sql` | Crea la tabla de ejemplo + 2 filas de prueba |
| `openwa_config.php` | **Configuración** (acá pegás la API key y el session id) |
| `openwa_client.php` | El que manda el mensaje a OpenWA (cURL) |
| `recordatorios_lib.php` | Conexión a BD, pendientes, enviados, log |
| `send_reminders.php` | El mensajero por línea de comandos (cron / dispatch) |
| `reminders_api.php` | La "puerta" que usa la app (pending / send / dispatch / crear / reset) |

### Pasos

1. **Importar la tabla** (solo si no tenés tu propia tabla):

   ```bash
   mysql -u root openwa_demo < recordatorios.sql
   ```

   > `openwa_demo` es solo un **nombre de ejemplo**. Usá el nombre de tu propia base
   > de datos y poné ese mismo nombre en `DB_NAME` dentro de `openwa_config.php`.

2. **Configurar** — abrí `openwa_config.php` y completá las líneas marcadas
   `CAMBIAR ACÁ`:
   - `OPENWA_API_KEY`
   - `OPENWA_SESSION_ID`
   - `DB_NAME`, `DB_USER`, `DB_PASS`

3. **Probar sin enviar nada** (dryrun):

   ```bash
   php backend/send_reminders.php dryrun
   ```

4. **Probar de verdad** (poné tu número en la tabla primero):

   ```bash
   php backend/send_reminders.php
   ```

5. **Opcional — automático sin cron:** ya está activo. Cada vez que la app pide los
   pendientes, el backend lanza el envío **una vez por día** (lazy sweep). Si preferís
   cron, agregá:

   ```bash
   # Linux
   0 8 * * * /usr/bin/php /ruta/a/tu-backend/send_reminders.php
   ```

---

## Android (Java + XML)

`android/` → copiá los 4 archivos a tu proyecto.

| Archivo | Dónde va |
|---|---|
| `Recordatorio.java` | el package de tu app (ej. `com.tuapp`) |
| `RecordatoriosApi.java` | el package de tu app |
| `RecordatoriosActivity.java` | el package de tu app |
| `activity_recordatorios.xml` | `res/layout/` |

### Pasos

1. **Cambiar el package.** Los 3 `.java` dicen `package com.tuapp;`. Cambiá esa línea
   por el package de tu proyecto.

2. **Pegar la URL del backend.** En `RecordatoriosApi.java`, cambiá:

   ```java
   private static final String BASE = "http://TU-SERVIDOR/tu-backend/";
   ```

   Por la URL real. En **XAMPP** el backend va en `htdocs/tu-backend/` y si
   probás desde el **emulador de Android Studio** queda:

   ```java
   private static final String BASE = "http://10.0.2.2/tu-backend/";
   ```

   - **Emulador** → `10.0.2.2` (alias de tu PC; `localhost` adentro del emulador es el emulador).
   - **Celular en la misma red** → la IP de tu PC (ej. `http://192.168.0.10/tu-backend/`).
   - Siempre **con la barra final.**

3. **Permisos** en `AndroidManifest.xml`, dentro de `<manifest>`:

   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```

   Y si tu backend es `http://` (sin candado), en `<application>`:

   ```xml
   <application
       ...
       android:usesCleartextTraffic="true">
   ```

   > `usesCleartextTraffic` es solo para desarrollo. En producción usá `https://`.

4. **Registrar la Activity** en `AndroidManifest.xml`, dentro de `<application>`:

   ```xml
   <activity android:name=".RecordatoriosActivity" />
   ```

   (o la lanzás desde tu menú/pantalla con `startActivity(new Intent(this, RecordatoriosActivity.class));`)

5. **Correr.** Tocá "Enviar pendientes" (funcionalidad 1), "Abrir WhatsApp" (2), o
   simplemente abrí la pantalla (3, automática).

---

## Si algo no anda

Mirá el orden de troubleshooting de la [guía, sección 8 y 9](GUIA-OPENWA-INTEGRACION.md#como-probar-sin-romper-nada).
El log del backend queda en `backend/logs/recordatorios.log` — ahí está escrito todo lo
que pasó.

**Nota:** `RecordatoriosActivity` usa `androidx.appcompat.app.AppCompatActivity`. Si tu
proyecto no usa AndroidX, cambialo por `android.app.Activity`.
