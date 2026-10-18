<?php
// openwa-demo/backend/send_reminders.php
//
// El "mensajero automático". Se ejecuta POR LÍNEA DE COMANDOS (no en el navegador):
//
//   php send_reminders.php            -> envía los pendientes
//   php send_reminders.php dryrun     -> SIMULA: muestra qué enviaría, sin enviar
//
// Lo llaman dos cosas: el cron (todos los días a una hora) y el botón
// "Enviar pendientes" de la app (a través de reminders_api.php?action=dispatch).

// Por seguridad, solo se puede ejecutar desde la terminal
if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit('Este archivo solo se ejecuta desde la línea de comandos');
}

require_once __DIR__ . '/openwa_config.php';
require_once __DIR__ . '/openwa_client.php';
require_once __DIR__ . '/recordatorios_lib.php';

$esSimulacion = isset($argv[1]) && strtolower($argv[1]) === 'dryrun';

// Interruptor general
if (RECORDATORIOS_ACTIVOS === false) {
    escribirLog('Interruptor apagado (RECORDATORIOS_ACTIVOS=false). No se envió nada.');
    exit(0);
}

$conn = conectar();
$pendientes = buscarPendientes($conn);
escribirLog('Pendientes encontrados: ' . count($pendientes));

// Modo simulación: no envía ni marca nada, solo muestra
if ($esSimulacion) {
    echo "=== SIMULACIÓN (no se envía ni se marca nada) ===\n";
    if (empty($pendientes)) {
        echo "No hay recordatorios pendientes.\n";
    }
    foreach ($pendientes as $r) {
        echo "#{$r['id']} -> {$r['telefono']} : {$r['mensaje']}\n";
    }
    exit(0);
}

// Envío real
$enviados = 0;
$fallidos = 0;
$total    = count($pendientes);

foreach ($pendientes as $posición => $r) {
    $resultado = enviarWhatsApp($r['telefono'], $r['mensaje']);

    if ($resultado['ok']) {
        marcarEnviado($conn, $r['id']);
        $enviados++;
        escribirLog("Enviado #{$r['id']} a {$r['telefono']}");
    } else {
        $fallidos++;
        // Si falla, NO lo marcamos: queda pendiente para la próxima.
        escribirLog("Falló #{$r['id']}: {$resultado['mensaje']}");
    }

    // Pausa entre mensajes (menos "robótico" = menos riesgo de bloqueo)
    if ($posición < $total - 1) {
        sleep(rand(20, 60));
    }
}

escribirLog("Resumen: enviados=$enviados, fallidos=$fallidos");
exit(0);
