<?php
// send_reminders.php
// Dispatcher de recordatorios automáticos de WhatsApp.
//
// Uso:
//   php send_reminders.php                  -> modo cron (distribuye en la ventana 08:00-13:00)
//   php send_reminders.php manual           -> disparo manual (pacing compacto fuera de ventana)
//   php send_reminders.php dryrun [manual]  -> prueba sin enviar: lista candidatos y arma mensajes
//
// Reglas:
//   - Solo turnos SCHEDULED a DÍAS(REMINDER_DAY_OFFSET) de distancia y no recordados.
//   - Distribución uniforme en la ventana horaria con jitter (nunca fijo "1 por segundo").
//   - Anti-ráfaga: lote > 15 => el lote se estira al menos 90 min.
//   - Reintento único a los REMINDER_RETRY_DELAY_SECONDS si falla el envío.
//   - Solo advierte, nunca bloquea: un fallo NO marca el turno como enviado.
//   - Log en backend/logs/reminders.log.

if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit('Este script solo puede ejecutarse por línea de comandos');
}

require_once __DIR__ . '/db_config.php';
require_once __DIR__ . '/config_reminders.php';
require_once __DIR__ . '/whatsapp_sender.php';

set_time_limit(0);

// ---------------------------------------------------------------------------
// Lock anti-duplicado (evita dos dispatches simultáneos)
// ---------------------------------------------------------------------------

$lockFilePath = __DIR__ . '/logs/reminders.lock';

function acquireLock($lockFilePath) {
    if (!is_dir(dirname($lockFilePath))) {
        mkdir(dirname($lockFilePath), 0777, true);
    }
    if (file_exists($lockFilePath)) {
        $prevPid = (int)trim((string)file_get_contents($lockFilePath));
        if ($prevPid > 0 && isProcessRunning($prevPid)) {
            return false;
        }
        if (time() - filemtime($lockFilePath) > 18000) {
            @unlink($lockFilePath);
        }
    }
    file_put_contents($lockFilePath, getmypid());
    return true;
}

function isProcessRunning($pid) {
    if ($pid <= 0) {
        return false;
    }
    if (PHP_OS_FAMILY === 'Windows') {
        $output = [];
        exec('tasklist /FI "PID eq ' . (int)$pid . '" /NH 2>NUL', $output);
        foreach ($output as $line) {
            if (preg_match('/\b' . (int)$pid . '\b/', $line)) {
                return true;
            }
        }
        return false;
    }
    return file_exists('/proc/' . (int)$pid);
}

function releaseLock($lockFilePath) {
    if (file_exists($lockFilePath) && (int)trim((string)file_get_contents($lockFilePath)) === (int)getmypid()) {
        @unlink($lockFilePath);
    }
}

// ---------------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------------

function logReminder($message) {
    $line = '[' . date('Y-m-d H:i:s') . '] ' . $message . PHP_EOL;
    if (!is_dir(dirname(REMINDER_LOG_PATH))) {
        mkdir(dirname(REMINDER_LOG_PATH), 0777, true);
    }
    @file_put_contents(REMINDER_LOG_PATH, $line, FILE_APPEND | LOCK_EX);
}

function isWithinWindow($nowTs) {
    $start = strtotime(REMINDER_WINDOW_START . ':00');
    $end   = strtotime(REMINDER_WINDOW_END . ':00');
    return $nowTs >= $start && $nowTs <= $end;
}

function windowStartToday() {
    return strtotime(REMINDER_WINDOW_START . ':00');
}

function windowEndToday() {
    return strtotime(REMINDER_WINDOW_END . ':00');
}

// Tope diario por warm-up, según días transcurridos desde el alta del número.
// Si no se conoce la fecha de alta, aplica el tope del día 1.
function warmupDailyMax($warmupStartDate = null) {
    $maxCap = defined('REMINDER_DAILY_MAX') ? REMINDER_DAILY_MAX : 50;
    if ($warmupStartDate === null) {
        return min($maxCap, WARMUP_DAILY_MAX_DAY1);
    }
    $daysActive = max(0, (int)floor((time() - strtotime($warmupStartDate)) / 86400));
    return min($maxCap, WARMUP_DAILY_MAX_DAY1 + ($daysActive * WARMUP_DAILY_MAX_STEP));
}

function buildReminderMessage($donor, $procedureLabel, $dateText, $timeText) {
    $greetings = getReminderGreetings();
    $greeting  = $greetings[array_rand($greetings)];

    $donorName = trim(($donor['firstName'] ?? '') . ' ' . ($donor['lastName'] ?? ''));

    // Resolver los placeholders dentro del saludo elegido primero
    // (strtr no re-escanea los valores reemplazados).
    $greeting = strtr($greeting, ['{donorName}' => $donorName]);

    $replace = [
        '{greeting}'  => $greeting,
        '{donorName}' => $donorName,
        '{procedure}' => $procedureLabel,
        '{date}'      => $dateText,
        '{time}'      => $timeText
    ];
    return strtr(REMINDER_MESSAGE_TEMPLATE, $replace);
}

// Variables posicionales para la plantilla de Meta ({{1}}..{{4}}): nombre, procedimiento, fecha, hora.
function buildMetaParams($donor, $procedureLabel, $dateText, $timeText) {
    $donorName = trim(($donor['firstName'] ?? '') . ' ' . ($donor['lastName'] ?? ''));
    return [$donorName, $procedureLabel, $dateText, $timeText];
}

function procedureLabel($code) {
    $labels = [
        'PLASMA'     => 'PLASMA',
        'PLATELETS'  => 'PLAQUETAS',
        'BLOOD'      => 'SANGRE'
    ];
    $code = strtoupper((string)$code);
    return $labels[$code] ?? $code;
}

function formatDateEs($dateStr) {
    $ts = strtotime($dateStr);
    if ($ts === false) {
        return $dateStr;
    }
    $days = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
    return $days[(int)date('w', $ts)] . ' ' . date('d/m/Y', $ts);
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

$isManual = isset($argv[1]) && strtolower((string)$argv[1]) === 'manual';
$isDryRun = isset($argv[1]) && strtolower((string)$argv[1]) === 'dryrun'
    || isset($argv[2]) && strtolower((string)$argv[2]) === 'dryrun';
$exitCode = 0;

// Kill-switch: corte por configuración sin desvincular el número de WhatsApp.
if (defined('REMINDER_ENABLED') && REMINDER_ENABLED === false) {
    logReminder('[CRON] Kill-switch activo (REMINDER_ENABLED=false). No se envió nada.');
    exit(0);
}

if (!acquireLock($lockFilePath)) {
    logReminder('Dispatch abortado: ya hay otro proceso de envío en ejecución.');
    exit(0);
}

try {
    $offsetDate = date('Y-m-d', strtotime('+' . (int)REMINDER_DAY_OFFSET . ' days'));

    // Turnos candidatos: del día objetivo, agendados, sin recordatorio.
    $stmt = $conn->prepare(
        "SELECT a.id AS appointment_id,
                a.donor_id,
                a.appointment_date,
                a.appointment_time,
                a.target_procedure,
                d.first_name AS firstName,
                d.last_name AS lastName,
                dp.phone_number AS donor_phone
           FROM appointments a
           JOIN donors d ON d.id = a.donor_id
      LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1
          WHERE a.appointment_date = :targetDate
            AND a.status = 'SCHEDULED'
            AND a.is_reminder_sent = 0
       ORDER BY a.appointment_time ASC"
    );
    $stmt->execute(['targetDate' => $offsetDate]);
    $candidates = $stmt->fetchAll();

    $total = count($candidates);
    $modeLabel = $isDryRun ? '[DRYRUN]' : ($isManual ? '[MANUAL]' : '[CRON]');
    logReminder($modeLabel . " Objetivo: $offsetDate | Candidatos: $total");

    if ($isDryRun) {
        echo "=== DRY RUN (no se envía ni se marca nada) ===" . PHP_EOL;
        echo "Objetivo: $offsetDate | Candidatos: $total" . PHP_EOL;
        foreach ($candidates as $c) {
            $msg = buildReminderMessage(
                $c,
                procedureLabel($c['target_procedure']),
                formatDateEs($c['appointment_date']),
                substr((string)$c['appointment_time'], 0, 5)
            );
            printf(
                "  #%d | donor_id %s | teléfono: %s (%s) | %s %s\n",
                (int)$c['appointment_id'],
                (string)$c['donor_id'],
                normalizeWaNumber($c['donor_phone']),
                ($c['donor_phone'] ?? '') === '' || trim($c['donor_phone']) === '' ? 'SIN TELÉFONO' : 'ok',
                (string)$c['appointment_date'],
                (string)$c['appointment_time']
            );
            echo "     -> " . $msg . PHP_EOL;
        }
        if ($total === 0) {
            echo "Sin turnos pendientes." . PHP_EOL;
        }
        exit(0);
    }

    if ($total === 0) {
        logReminder('Sin turnos pendientes de recordatorio. Fin.');
        $exitCode = 0;
    } else {
        $sentCount      = 0;
        $failedCount    = 0;
        $warmupLimit    = warmupDailyMax();
        $processed      = 0;
        $nowTs          = time();

        // Fecha de inicio del warm-up (si existe en la tabla de control)
        try {
            $warmupStart = null;
            $ctrlStmt = $conn->query("SELECT warmup_start_date FROM reminder_dispatch_control WHERE id = 1");
            $ctrlRow  = $ctrlStmt->fetch();
            if ($ctrlRow && !empty($ctrlRow['warmup_start_date'])) {
                $warmupStart = $ctrlRow['warmup_start_date'];
            }
            $warmupLimit = warmupDailyMax($warmupStart);
        } catch (Throwable $ignored) {
            // si la tabla no existe aún, se usa el tope del día 1
        }

        // --- Modo cron: iniciar al comienzo de la ventana aunque el cron llegue antes ---
        $startTs = windowStartToday();
        $endTs   = windowEndToday();

        if ($nowTs > $startTs && $nowTs < $endTs) {
            $startTs = $nowTs; // ya empezó la ventana
        }
        if ($nowTs >= $endTs || $nowTs < $startTs) {
            // Fuera de ventana (cron retrasado o disparo manual nocturno):
            // pacing compacto con anti-ráfaga, no esperamos mañana.
            $mode = 'compact';
        } else {
            $mode = 'window';
        }

        $spreadSeconds = $endTs - $startTs;

        foreach ($candidates as $i => $candidate) {
            $processed++;
            if ($processed > $warmupLimit) {
                logReminder("Warm-up del día alcanzado ($warmupLimit). Quedan pendientes para próximas corridas.");
                break;
            }

            if ($isManual && $mode === 'compact') {
                // Pacing compacto manual con anti-ráfaga para lotes grandes
                $slotDuration = 0;
                if (count($candidates) > LARGE_BATCH_THRESHOLD) {
                    $slotDuration = max(0.0, (BATCH_SPREAD_MIN_SECONDS / count($candidates)) - MANUAL_DELAY_MIN);
                }
                $minDelay = MANUAL_DELAY_MIN + (int)$slotDuration;
                $maxDelay = MANUAL_DELAY_MAX + (int)$slotDuration;
                $targetTs = time() + rand($minDelay, $maxDelay);
            } elseif ($mode === 'window') {
                // Distribución uniforme del lote dentro de la ventana con jitter
                $slot = (int)round(($spreadSeconds * ($i + 1)) / ($total + 1));
                $jitter = (int)round(($spreadSeconds / ($total + 1)) * 0.2 * (mt_rand(-100, 100) / 100.0));
                $slot = max(0, $slot + $jitter);
                $targetTs = $startTs + $slot;
            } else {
                $targetTs = time() + rand(MANUAL_DELAY_MIN, MANUAL_DELAY_MAX);
            }

            if ($targetTs > time()) {
                sleep($targetTs - time());
            }

            $appointmentId = (int)$candidate['appointment_id'];
            $phoneDigits   = normalizeWaNumber($candidate['donor_phone']);

            if ($phoneDigits === '') {
                logReminder("Turno #$appointmentId (donor_id {$candidate['donor_id']}): SIN teléfono registrado. Se omite, no se marca como enviado.");
                continue;
            }

            $procedure  = procedureLabel($candidate['target_procedure']);
            $dateText   = formatDateEs($candidate['appointment_date']);
            $timeText   = substr((string)$candidate['appointment_time'], 0, 5);
            $message    = buildReminderMessage($candidate, $procedure, $dateText, $timeText);
            $metaParams = buildMetaParams($candidate, $procedure, $dateText, $timeText);

            $result = sendWhatsappText($phoneDigits, $message, $metaParams);

            if (!$result['ok'] && defined('REMINDER_RETRY_DELAY_SECONDS') && REMINDER_RETRY_DELAY_SECONDS > 0) {
                logReminder("Turno #$appointmentId falló: {$result['message']}. Reintento en " . REMINDER_RETRY_DELAY_SECONDS . 's');
                sleep((int)REMINDER_RETRY_DELAY_SECONDS);
                $result = sendWhatsappText($phoneDigits, $message, $metaParams);
            }

            if ($result['ok']) {
                $sentCount++;
                $update = $conn->prepare(
                    "UPDATE appointments SET is_reminder_sent = 1, reminder_sent_at = NOW(), reminder_sent_by_user_id = NULL WHERE id = :id"
                );
                $update->execute(['id' => $appointmentId]);
                logReminder("Turno #$appointmentId -> ENVIADO a $phoneDigits");
            } else {
                $failedCount++;
                $exitCode = 1;
                logReminder("Turno #$appointmentId FALLÓ: {$result['message']} (NO marcado como enviado; quedará pendiente)");
            }
        }

        logReminder("Resumen: enviados=$sentCount, fallidos=$failedCount, procesados=$processed");

        // Marcar la última ejecución (la usa el lazy sweep para no repetir en el día)
        try {
            $conn->prepare(
                "INSERT INTO reminder_dispatch_control (id, last_run_at, updated_at)
                 VALUES (1, NOW(), NOW())
                 ON DUPLICATE KEY UPDATE last_run_at = NOW(), updated_at = NOW()"
            )->execute();
        } catch (Throwable $ignored) {
            // la tabla de control puede no existir aún; no bloquea el envío
        }
    }
} catch (Throwable $e) {
    $exitCode = 1;
    logReminder('Error crítico: ' . $e->getMessage());
} finally {
    releaseLock($lockFilePath);
}

exit($exitCode);
?>