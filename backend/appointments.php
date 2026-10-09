<?php
require_once 'db_config.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $weekDate = trim($_GET['week_date'] ?? $_GET['date'] ?? date('Y-m-d'));
    $month = trim($_GET['month'] ?? '');

    $baseSelect = "SELECT a.id, a.donor_id AS donorId, CONCAT(d.first_name, ' ', d.last_name) AS donorName, dp.phone_number AS donorPhone, dem.email_address AS donorEmail, a.appointment_date AS appointmentDate, DATE_FORMAT(a.appointment_time, '%H:%i') AS appointmentTime, a.target_procedure AS targetProcedure, a.status, a.is_reminder_sent AS isReminderSent, a.is_email_reminder_sent AS isEmailReminderSent, a.reminder_sent_at AS reminderSentAt FROM appointments a JOIN donors d ON a.donor_id = d.id LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1 LEFT JOIN donor_emails dem ON dem.donor_id = d.id AND dem.is_primary = 1";

    try {
        if ($month !== '' && preg_match('/^\d{4}-\d{2}$/', $month)) {
            $startDate = $month . '-01';
            $endDate = date('Y-m-t', strtotime($startDate));
            $stmt = $conn->prepare($baseSelect . " WHERE a.appointment_date BETWEEN :startDate AND :endDate ORDER BY a.appointment_date ASC, a.appointment_time ASC");
            $stmt->execute(['startDate' => $startDate, 'endDate' => $endDate]);
        } else {
            $stmt = $conn->prepare($baseSelect . " WHERE a.appointment_date >= :weekDate ORDER BY a.appointment_date ASC, a.appointment_time ASC");
            $stmt->execute(['weekDate' => $weekDate]);
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
            $stmt = $conn->prepare("SELECT id FROM donors WHERE id = :id");
            $stmt->execute(['id' => $donorId]);
            if (!$stmt->fetch()) {
                sendJsonResponse("error", "No se encontró el donante seleccionado", null, 404);
            }

            $stmt = $conn->prepare("SELECT id FROM appointments WHERE donor_id = :donorId AND appointment_date = :date AND status <> 'CANCELLED'");
            $stmt->execute(['donorId' => $donorId, 'date' => $appointmentDate]);
            if ($stmt->fetch()) {
                sendJsonResponse("error", "El donante ya tiene un turno agendado para ese día", null, 409);
            }

            $stmt = $conn->prepare("INSERT INTO appointments (donor_id, appointment_date, appointment_time, target_procedure, status) VALUES (:donorId, :date, :time, :procedure, 'SCHEDULED')");
            $stmt->execute([
                'donorId' => $donorId,
                'date' => $appointmentDate,
                'time' => $appointmentTime,
                'procedure' => $targetProcedure
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