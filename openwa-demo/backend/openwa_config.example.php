<?php
// openwa-demo/backend/openwa_config.example.php
//
// PLANTILLA. Copiá este archivo como "openwa_config.php" y completá lo que
// está marcado con "CAMBIAR ACÁ". El archivo real vive en el servidor y no
// se sube al repo (tiene la API key y el session id).

// --- OpenWA ---------------------------------------------------------
// CAMBIAR ACÁ: la URL donde corre OpenWA (puerto por defecto: 2785)
define('OPENWA_BASE_URL', 'http://localhost:2785');
// CAMBIAR ACÁ: la API key que OpenWA mostró en el primer arranque
define('OPENWA_API_KEY', 'TU_API_KEY');
// CAMBIAR ACÁ: el id de la sesión que vinculaste escaneando el QR
define('OPENWA_SESSION_ID', 'TU_SESSION_ID');

// --- Interruptor general (kill-switch) ------------------------------
// Si lo ponés en false, no se envía NADA. Sirve para "apagar" rápido.
define('RECORDATORIOS_ACTIVOS', true);

// --- Base de datos --------------------------------------------------
// CAMBIAR ACÁ: los datos de tu MySQL (XAMPP por defecto: root sin contraseña).
// "openwa_demo" es solo un nombre de EJEMPLO: poné el de tu propia base.
define('DB_HOST', 'localhost');
define('DB_NAME', 'openwa_demo');
define('DB_USER', 'root');
define('DB_PASS', '');

// --- Log ------------------------------------------------------------
// Ahí se anota todo lo que pasa. No hace falta tocar.
define('LOG_PATH', __DIR__ . '/logs/recordatorios.log');

// --- Ruta al php.exe (para lanzar en segundo plano) -----------------
// CAMBIAR ACÁ si tu XAMPP/PHP vive en otro lado. Se usa para correr
// send_reminders.php en segundo plano (dispatch y lazy sweep).
// Si lo dejás vacío (''), se intenta detectar solo.
// XAMPP en Windows suele ser: C:\xampp\php\php.exe
define('PHP_CLI_PATH', '');
