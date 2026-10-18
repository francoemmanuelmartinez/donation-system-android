<?php
// openwa-demo/backend/reminders_api.php
//
// La "puerta" que usa la app Android. Siempre responde JSON.
//
//   GET  reminders_api.php?action=pending    -> lista los pendientes
//   POST reminders_api.php?action=send       -> envía UNO por su id  (body: {"id":1})
//   POST reminders_api.php?action=dispatch   -> dispara TODOS en segundo plano
//   POST reminders_api.php?action=crear       -> crea registro con programado_para
//   POST reminders_api.php?action=reset       -> reinicia gate lazy sweep (demo)
require_once __DIR__ . '/openwa_config.php';
require_once __DIR__ . '/openwa_client.php';
require_once __DIR__ . '/recordatorios_lib.php';

header('Content-Type: application/json; charset=UTF-8');

// Ayudantes para responder siempre igual
function responder($datos) {
    echo json_encode(['ok' => true, 'data' => $datos], JSON_UNESCAPED_UNICODE);
    exit;
}
function responderError($mensaje, $codigo = 400) {
    http_response_code($codigo);
    echo json_encode(['ok' => false, 'error' => $mensaje], JSON_UNESCAPED_UNICODE);
    exit;
}

// Busca un php.exe que realmente exista. Bajo Apache, PHP_BINDIR a veces apunta a
// una instalación distinta (ej. C:\php) y "start" tira un cartel de error de Windows.
// Orden: PHP_CLI_PATH (configurado a mano) -> PHP_BINDIR -> php del PATH.
function resolverPhpCli() {
    if (defined('PHP_CLI_PATH') && PHP_CLI_PATH !== '' && file_exists(PHP_CLI_PATH)) {
        return PHP_CLI_PATH;
    }

    $enBindir = PHP_BINDIR . DIRECTORY_SEPARATOR . (PHP_OS_FAMILY === 'Windows' ? 'php.exe' : 'php');
    if (file_exists($enBindir)) {
        return $enBindir;
    }

    // Último recurso: que lo resuelva el PATH del sistema
    return (PHP_OS_FAMILY === 'Windows') ? 'php.exe' : 'php';
}

// Lanza send_reminders.php en segundo plano y responde ya, sin hacer esperar a la app.
function lanzarEnSegundoPlano() {
    $php   = resolverPhpCli();
    $script = __DIR__ . '/send_reminders.php';

    if (PHP_OS_FAMILY === 'Windows') {
        // Si ni siquiera encontramos un php.exe válido, no intentamos lanzar:
        // evita el cartel de error de Windows y el error queda anotado en el log.
        if ($php === 'php.exe' || !file_exists($php)) {
            escribirLog("No se encontró un php.exe válido (revisá PHP_CLI_PATH). No se lanzó el envío.");
            return;
        }
        // Redirigimos la salida del hijo a NUL para que el pipe de popen se cierre
        // al instante (si no, pclose esperaría a que termine todo el envío).
        // "<NUL" evita que herede la entrada y quede colgado esperando.
        $cmd = 'start "recordatorios" /B cmd /c ' . escapeshellarg(
            '"' . $php . '" "' . $script . '" <NUL >NUL 2>&1'
        );
        pclose(popen($cmd, 'r'));
    } else {
        exec(escapeshellarg($php) . ' ' . escapeshellarg($script) . ' > /dev/null 2>&1 &');
    }
}

// Plan B (lazy sweep): si NO hay cron configurado, aprovecha que la app pide algo
// y lanza los envíos UNA VEZ por día.
function quizasLanzarEnvios($conn) {
    if (RECORDATORIOS_ACTIVOS === false) {
        return;
    }

    // Control: solo una vez por día (la fecha queda guardada en un archivito)
    $sello = __DIR__ . '/logs/ultimo_envio.txt';
    $hoy = date('Y-m-d');
    if (file_exists($sello) && trim(file_get_contents($sello)) === $hoy) {
        return; // ya se lanzó hoy
    }

    if (count(buscarPendientes($conn)) === 0) {
        return;
    }

    if (!is_dir(dirname($sello))) {
        mkdir(dirname($sello), 0777, true);
    }
    file_put_contents($sello, $hoy);

    lanzarEnSegundoPlano();
}

$accion = $_GET['action'] ?? '';
$conn = conectar();

if ($accion === 'pending') {
    // Plan B: si no hay cron, esta llamada puede disparar el envío del día
    quizasLanzarEnvios($conn);
    responder(buscarPendientes($conn));

} elseif ($accion === 'send') {
    // Enviar UNO por su id (lo dispara el botón de un item)
    $cuerpo = json_decode(file_get_contents('php://input'), true);
    $id = (int)($cuerpo['id'] ?? 0);
    if ($id <= 0) {
        responderError('Falta el id del recordatorio');
    }

    $stmt = $conn->prepare("SELECT id, telefono, mensaje FROM recordatorios WHERE id = :id");
    $stmt->execute(['id' => $id]);
    $r = $stmt->fetch();
    if (!$r) {
        responderError('No existe ese recordatorio', 404);
    }

    $resultado = enviarWhatsApp($r['telefono'], $r['mensaje']);
    if (!$resultado['ok']) {
        responderError($resultado['mensaje'], 500);
    }

    marcarEnviado($conn, $id);
    responder(['id' => $id, 'enviado' => true]);

} elseif ($accion === 'dispatch') {
    // Disparar TODOS los pendientes en segundo plano (botón "Enviar pendientes")
    lanzarEnSegundoPlano();
    responder(['dispatched' => true]);

} elseif ($accion === 'crear') {
    $c = json_decode(file_get_contents('php://input'), true);
    $nombre   = trim($c['nombre'] ?? '');
    $telefono = trim($c['telefono'] ?? '');
    $mensaje  = trim($c['mensaje']  ?? '');
    $minutos  = (int)($c['minutos']  ?? 0);

    if ($nombre === '' || $telefono === '' || $mensaje === '') {
        responderError('Faltan nombre, teléfono o mensaje');
    }
    if ($minutos < 0) {
        $minutos = 0;
    }

    // La hora se calcula con NOW() de la base (no con date() de PHP) para que
    // coincida con la comparación "programado_para <= NOW()" de buscarPendientes().
    $programado = $conn->query("SELECT DATE_ADD(NOW(), INTERVAL " . $minutos . " MINUTE)")->fetchColumn();
    $id = crearRecordatorio($conn, $nombre, $telefono, $mensaje, $programado);
    responder(['id' => $id, 'programado_para' => $programado, 'minutos' => $minutos]);

} elseif ($accion === 'reset') {
    $sello = __DIR__ . '/logs/ultimo_envio.txt';
    if (file_exists($sello)) {
        @unlink($sello);
    }
    escribirLog('Gate del lazy sweep reiniciado (ultimo_envio.txt borrado).');
    responder(['reset' => true]);

} else {
    responderError('Acción desconocida. Usá: pending, send, dispatch, crear o reset');
}
