<?php
// config_reminders.php
// Configuración de recordatorios automáticos de WhatsApp para donantes.
// Todos los valores son editables sin tocar el código.

define('REMINDER_PROVIDER', 'openwa');            // 'openwa' (gratis, activo hoy) | 'meta' (oficial, listo — falta credencial)

// --- Kill-switch: con false no se envía NADA (corte por config, no desvincula el número) ---
define('REMINDER_ENABLED', true);

// --- Ventana horaria diaria de envío (el banco atiende de 8 a 13hs) ---
define('REMINDER_WINDOW_START', '08:00');
define('REMINDER_WINDOW_END',   '13:00');

// --- Cuántos días antes del turno se envía el recordatorio ---
define('REMINDER_DAY_OFFSET', 2);

// --- Texto del recordatorio: {greeting}, {donorName}, {procedure}, {date}, {time} ---
define('REMINDER_MESSAGE_TEMPLATE', '{greeting} Tenés un turno de {procedure} el {date} a las {time}.');

// --- Saludos alternativos (se elige uno al azar por mensaje para que sea único) ---
$REMINDER_GREETINGS = [
    'Hola {donorName}!',
    'Hola {donorName}, ¿cómo estás?',
    'Estimado/a {donorName}:',
    '{donorName}, te esperamos en el Banco de Sangre.'
];

// --- OpenWA (proveedor actual) ---
// Las credenciales NO se versionan: se definen en config_reminders_override.php
// (gitignored). Sesión vinculada al chip dedicado 5492233556122 (cambio 12-oct-2026).
if (file_exists(__DIR__ . '/config_reminders_override.php')) {
    require_once __DIR__ . '/config_reminders_override.php';
}

if (!defined('OPENWA_BASE_URL'))   define('OPENWA_BASE_URL', 'http://localhost:2785');
if (!defined('OPENWA_API_KEY'))    define('OPENWA_API_KEY', '');
if (!defined('OPENWA_SESSION_ID')) define('OPENWA_SESSION_ID', '');

// --- API oficial de Meta (Cloud API) - EN PAUSA (12-oct-2026) ---
// Fase 2 implementada pero en pausa por decisión del usuario: el Business Portfolio quedó
// restringido por hold de cuenta de Facebook nueva. OpenWA es el proveedor operativo.
// Se usa solo si REMINDER_PROVIDER = 'meta'. Plantilla UTILITY: recordatorio_turno (es_AR).
// Con token/número vacíos, sendViaMeta() devuelve error claro sin romper el dispatcher.
define('META_API_VERSION', 'v21.0');
define('META_TOKEN', '');
define('META_PHONE_NUMBER_ID', '');
define('META_TEMPLATE_NAME', 'recordatorio_turno');
define('META_TEMPLATE_LANGUAGE', 'es_AR');

// --- Ruta del binario PHP-CLI ---
// Necesario para lanzar send_reminders.php en background desde Apache
// (bajo Apache, PHP_BINARY apunta a httpd.exe, no a php.exe).
// Dejar vacío para autodetección (soporta XAMPP y Linux con "php" en PATH).
define('PHP_CLI_PATH', '');

// --- Anti-ráfaga / pacing ---
define('MANUAL_DELAY_MIN', 20);        // segundos, disparo manual fuera de la ventana
define('MANUAL_DELAY_MAX', 60);
define('BATCH_SPREAD_MIN_SECONDS', 5400); // lote > 15 mensajes: mínimo 90 min totales
define('LARGE_BATCH_THRESHOLD', 15);

// --- Warm-up para números nuevos: tope diario progresivo ---
define('WARMUP_DAILY_MAX_DAY1', 10);   // primer día
define('WARMUP_DAILY_MAX_STEP', 10);   // +10 por día hasta el tope máximo
define('REMINDER_DAILY_MAX', 50);      // tope absoluto de recordatorios por día

// --- Reintentos / fallos ---
define('REMINDER_RETRY_DELAY_SECONDS', 300); // reintento único si falla el envío
define('REMINDER_FAIL_IF_NO_PHONE', false);  // true = marca error en log | false = solo advierte y omite

// --- Log ---
define('REMINDER_LOG_PATH', __DIR__ . '/logs/reminders.log');

function getReminderGreetings() {
    global $REMINDER_GREETINGS;
    return $REMINDER_GREETINGS;
}

/**
 * Resuelve la ruta del binario PHP-CLI para lanzar send_reminders.php en background.
 * Prioridad: PHP_CLI_PATH configurado > PHP_BINARY (si es CLI) > autodetección.
 */
function resolvePhpCliBinary() {
    if (defined('PHP_CLI_PATH') && PHP_CLI_PATH !== '' && file_exists(PHP_CLI_PATH)) {
        return PHP_CLI_PATH;
    }

    // Si ya corremos por CLI, PHP_BINARY es el propio php.exe
    if (PHP_SAPI === 'cli' && PHP_BINARY && file_exists(PHP_BINARY)) {
        return PHP_BINARY;
    }

    if (PHP_OS_FAMILY === 'Windows') {
        $candidates = [];

        // XAMPP: PHP_BINARY bajo Apache es <xampp>\apache\bin\httpd.exe
        // -> tres niveles arriba está <xampp>, y php.exe vive en <xampp>\php\php.exe
        if (PHP_BINARY) {
            $candidates[] = dirname(dirname(dirname(PHP_BINARY))) . '\\php\\php.exe';
        }

        $candidates[] = 'C:\\xampp\\php\\php.exe';
        $candidates[] = dirname(PHP_BINDIR) . '\\php\\php.exe';
        $candidates[] = 'C:\\php\\php.exe';
        $candidates[] = 'php.exe';

        foreach ($candidates as $candidate) {
            if ($candidate === 'php.exe' || file_exists($candidate)) {
                return $candidate;
            }
        }
        return 'php.exe';
    }

    return 'php';
}
?>