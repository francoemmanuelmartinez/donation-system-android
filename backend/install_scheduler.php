<?php
// install_scheduler.php
// Instala el disparo automático de recordatorios según el SO del servidor.
// Se ejecuta UNA sola vez por línea de comandos:
//   php install_scheduler.php
//
// Windows -> Task Scheduler (schtasks), tarea "RecordatoriosDonacion" diaria a las 08:00
// Linux   -> crontab del usuario, línea "0 8 * * *"
//
// Requiere permisos: en Windows, una terminal con permisos de administrador.

if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit('Este script solo puede ejecutarse por línea de comandos');
}

require_once __DIR__ . '/config_reminders.php';

$startTime = REMINDER_WINDOW_START; // "08:00"
$scriptPath = realpath(__DIR__ . '/send_reminders.php');
if ($scriptPath === false) {
    fwrite(STDERR, "No se encontró send_reminders.php" . PHP_EOL);
    exit(1);
}

$hour = (int)substr($startTime, 0, 2);
$min  = (int)substr($startTime, 3, 2);

if (PHP_OS_FAMILY === 'Windows') {
    $phpBin = defined('PHP_BINARY') && PHP_BINARY !== '' ? PHP_BINARY : 'php';
    $taskName = 'RecordatoriosDonacion';
    $cmd = sprintf(
        'schtasks /Create /F /TN "%s" /SC DAILY /ST %02d:%02d /TR "%s"',
        $taskName,
        $hour,
        $min,
        '"' . $phpBin . '" -f "' . $scriptPath . '"'
    );
    echo "Ejecutando: $cmd" . PHP_EOL;
    passthru($cmd, $ret);
    echo "Resultado Task Scheduler: $ret" . PHP_EOL;
    if ($ret !== 0) {
        fwrite(STDERR, "Fallo el registro. Probablemente necesites una terminal de administrador." . PHP_EOL);
    }
    exit($ret);
}

// Linux / macOS
$cronLine = sprintf('%d %d * * * %s %s >> %s 2>&1', $min, $hour, 'php', $scriptPath, __DIR__ . '/logs/reminders.cron.log');

// Agregar al crontab del usuario sin duplicar la regla
$currentCron = trim((string)shell_exec('crontab -l 2>/dev/null'));
if (strpos($currentCron, $scriptPath) !== false) {
    echo "El cron ya contiene la regla de $scriptPath" . PHP_EOL;
    exit(0);
}

$newCron = $currentCron === '' ? $cronLine : $currentCron . PHP_EOL . $cronLine;
$tmpFile = tempnam(sys_get_temp_dir(), 'cron_');
file_put_contents($tmpFile, $newCron . PHP_EOL);
echo "Agregando al crontab: $cronLine" . PHP_EOL;
passthru('crontab ' . escapeshellarg($tmpFile), $ret);
@unlink($tmpFile);
echo "Resultado crontab: $ret" . PHP_EOL;
exit($ret);
?>