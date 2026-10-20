<?php
require_once 'db_config.php';
require_once 'config_reminders.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

// Lazy sweep: si hoy aún no corrió el dispatch automático y estamos en la
// ventana horaria laboral, lanzamos el envío automático una sola vez.
// Solo advierte, nunca bloquea: si falla, el turno sigue sin marcar.
function tryLazySweep($conn) {
    try {
        // Kill-switch: corte por configuración sin desvincular el número.
        if (defined('REMINDER_ENABLED') && REMINDER_ENABLED === false) {
            return;
        }

        // Lazy sweep desactivable por máquina: con false, abrir la Agenda no
        // dispara envíos; el único disparo es el botón (action=dispatch_reminders).
        if (defined('REMINDER_LAZY_SWEEP_ENABLED') && REMINDER_LAZY_SWEEP_ENABLED === false) {
            return;
        }

        $stmt = $conn->prepare("SELECT last_run_at FROM reminder_dispatch_control WHERE id = 1");
        $stmt->execute();
        $row = $stmt->fetch();

        $ranToday = $row && $row['last_run_at'] !== null && date('Y-m-d', strtotime($row['last_run_at'])) === date('Y-m-d');
        if ($ranToday) {
            return;
        }

        $now = time();
        $start = strtotime(REMINDER_WINDOW_START . ':00');
        $end   = strtotime(REMINDER_WINDOW_END . ':00');
        if ($now < $start || $now > $end) {
            return;
        }

        $sweepLock = __DIR__ . '/logs/reminders.sweep';
        if (file_exists($sweepLock) && (time() - filemtime($sweepLock) < 120)) {
            return; // otro request ya disparó el sweep hace menos de 2 minutos
        }
        @file_put_contents($sweepLock, time());

        $script = escapeshellarg(dirname(__FILE__) . '/send_reminders.php');
        $phpBin = resolvePhpCliBinary();
        $cmd = escapeshellarg($phpBin) . ' ' . $script;

        if (PHP_OS_FAMILY === 'Windows') {
            // Lanzar en background sin bloquear el request
            // start "" /B -> el primer argumento vacío evita que la ruta se tome como título
            pclose(popen('start "" /B ' . $cmd, 'r'));
        } else {
            exec($cmd . ' > /dev/null 2>&1 &');
        }
    } catch (Throwable $ignored) {
        // el sweep nunca bloquea el request
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    tryLazySweep($conn);

    $weekDate = trim($_GET['week_date'] ?? $_GET['date'] ?? date('Y-m-d'));
    $month = trim($_GET['month'] ?? '');
    $centerId = (int)($_GET['centerId'] ?? $_GET['center_id'] ?? 0);

    $baseSelect = "SELECT a.id, a.donor_id AS donorId, CONCAT(d.first_name, ' ', d.last_name) AS donorName, dp.phone_number AS donorPhone, dem.email_address AS donorEmail, a.appointment_date AS appointmentDate, DATE_FORMAT(a.appointment_time, '%H:%i') AS appointmentTime, a.target_procedure AS targetProcedure, a.status, a.is_reminder_sent AS isReminderSent, a.is_email_reminder_sent AS isEmailReminderSent, a.reminder_sent_at AS reminderSentAt FROM appointments a JOIN donors d ON a.donor_id = d.id LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1 LEFT JOIN donor_emails dem ON dem.donor_id = d.id AND dem.is_primary = 1";

    try {
        if ($month !== '' && preg_match('/^\d{4}-\d{2}$/', $month)) {
            $startDate = $month . '-01';
            $endDate = date('Y-m-t', strtotime($startDate));
            $sql = $baseSelect . " WHERE a.appointment_date BETWEEN :startDate AND :endDate";
            $params = ['startDate' => $startDate, 'endDate' => $endDate];
            if ($centerId > 0) {
                $sql .= " AND a.center_id = :centerId";
                $params['centerId'] = $centerId;
            }
            $sql .= " ORDER BY a.appointment_date ASC, a.appointment_time ASC";
            $stmt = $conn->prepare($sql);
            $stmt->execute($params);
        } else {
            $sql = $baseSelect . " WHERE a.appointment_date >= :weekDate";
            $params = ['weekDate' => $weekDate];
            if ($centerId > 0) {
                $sql .= " AND a.center_id = :centerId";
                $params['centerId'] = $centerId;
            }
            $sql .= " ORDER BY a.appointment_date ASC, a.appointment_time ASC";
            $stmt = $conn->prepare($sql);
            $stmt->execute($params);
        }
        $appointments = $stmt->fetchAll();

        foreach ($appointments as &$app) {
            $app['isReminderSent'] = (bool)$app['isReminderSent'];
            $app['isEmailReminderSent'] = (bool)$app['isEmailReminderSent'];
        }

        sendJsonResponse("success", "Agenda recuperada", $appointments);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data) {
        sendJsonResponse("error", "Invalid JSON payload", null, 400);
    }

    if ($action === 'send_reminder') {
        $appointmentId = (int)($data->appointmentId ?? $data->appointment_id ?? 0);
        $userId = isset($data->userId) ? (int)$data->userId : null;
        $channel = strtolower(trim($data->channel ?? 'whatsapp'));

        if ($appointmentId <= 0) {
            sendJsonResponse("error", "Invalid appointment ID", null, 400);
        }

        if ($channel !== 'whatsapp' && $channel !== 'email') {
            sendJsonResponse("error", "Canal de recordatorio inválido", null, 400);
        }

        try {
            $column = $channel === 'email' ? 'is_email_reminder_sent' : 'is_reminder_sent';
            $stmt = $conn->prepare("UPDATE appointments SET $column = 1, reminder_sent_at = NOW(), reminder_sent_by_user_id = :userId WHERE id = :id");
            $stmt->execute(['userId' => $userId, 'id' => $appointmentId]);

            sendJsonResponse("success", "Estado de recordatorio actualizado correctamente", [
                "appointmentId" => $appointmentId,
                "channel" => $channel,
                "isReminderSent" => $channel !== 'email'
            ]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } elseif ($action === 'dispatch_reminders') {
        // Disparo manual del envío automático (botón de la app / pruebas).
        // Reutiliza send_reminders.php por CLI; el request no se bloquea.
        try {
            if (defined('REMINDER_ENABLED') && REMINDER_ENABLED === false) {
                sendJsonResponse("success", "Kill-switch activo: envío de recordatorios deshabilitado", [
                    "dispatched" => false,
                    "mode" => "kill-switch"
                ]);
            }

            $manual = trim($data->manual ?? '') === 'true' ? 'manual' : '';
            $script = escapeshellarg(dirname(__FILE__) . '/send_reminders.php');
            $phpBin = resolvePhpCliBinary();
            $cmd = escapeshellarg($phpBin) . ' ' . $script . ($manual !== '' ? ' manual' : '');

            if (PHP_OS_FAMILY === 'Windows') {
                pclose(popen('start "" /B ' . $cmd, 'r'));
            } else {
                exec($cmd . ' > /dev/null 2>&1 &');
            }

            sendJsonResponse("success", "Dispatch de recordatorios lanzado correctamente", [
                "dispatched" => true,
                "mode" => $manual !== '' ? 'manual' : 'auto'
            ]);
        } catch (Throwable $e) {
            sendJsonResponse("error", "No se pudo lanzar el dispatch: " . $e->getMessage(), null, 500);
        }
    } elseif ($action === 'create_appointment') {
        $donorId = (int)($data->donorId ?? $data->donor_id ?? 0);
        $appointmentDate = trim($data->appointmentDate ?? $data->appointment_date ?? '');
        $appointmentTime = trim($data->appointmentTime ?? $data->appointment_time ?? '');
        $targetProcedure = strtoupper(trim($data->targetProcedure ?? $data->target_procedure ?? ''));

        if ($donorId <= 0) {
            sendJsonResponse("error", "Seleccioná un donante válido", null, 400);
        }

        $dateTime = DateTime::createFromFormat('Y-m-d', $appointmentDate);
        if (!$dateTime || $dateTime->format('Y-m-d') !== $appointmentDate) {
            sendJsonResponse("error", "Fecha de turno inválida", null, 400);
        }

        if (!preg_match('/^\d{1,2}:\d{2}(:\d{2})?$/', $appointmentTime)) {
            sendJsonResponse("error", "Hora de turno inválida", null, 400);
        }

        $allowedProcedures = ['BLOOD', 'PLASMA', 'PLATELETS'];
        if (!in_array($targetProcedure, $allowedProcedures)) {
            sendJsonResponse("error", "Tipo de donación inválido", null, 400);
        }

        try {
            $stmt = $conn->prepare("SELECT id, center_id FROM donors WHERE id = :id");
            $stmt->execute(['id' => $donorId]);
            $donorRow = $stmt->fetch();
            if (!$donorRow) {
                sendJsonResponse("error", "No se encontró el donante seleccionado", null, 404);
            }
            $centerId = (int)($donorRow['center_id'] ?? 0);
            if ($centerId <= 0) { $centerId = 1; }

            $stmt = $conn->prepare("SELECT id FROM appointments WHERE donor_id = :donorId AND appointment_date = :date AND status <> 'CANCELLED'");
            $stmt->execute(['donorId' => $donorId, 'date' => $appointmentDate]);
            if ($stmt->fetch()) {
                sendJsonResponse("error", "El donante ya tiene un turno agendado para ese día", null, 409);
            }

            $stmt = $conn->prepare("INSERT INTO appointments (donor_id, appointment_date, appointment_time, target_procedure, status, center_id) VALUES (:donorId, :date, :time, :procedure, 'SCHEDULED', :centerId)");
            $stmt->execute([
                'donorId' => $donorId,
                'date' => $appointmentDate,
                'time' => $appointmentTime,
                'procedure' => $targetProcedure,
                'centerId' => $centerId
            ]);

            sendJsonResponse("success", "Turno agendado correctamente", [
                "appointmentId" => (int)$conn->lastInsertId(),
                "donorId" => $donorId,
                "appointmentDate" => $appointmentDate,
                "appointmentTime" => $appointmentTime,
                "targetProcedure" => $targetProcedure
            ]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else {
        sendJsonResponse("error", "Invalid action", null, 400);
    }
}
?>