<?php
require_once 'db_config.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $weekDate = trim($_GET['week_date'] ?? $_GET['date'] ?? date('Y-m-d'));

    try {
        $stmt = $conn->prepare("SELECT a.id, a.donor_id AS donorId, CONCAT(d.first_name, ' ', d.last_name) AS donorName, dp.phone_number AS donorPhone, a.appointment_date AS appointmentDate, DATE_FORMAT(a.appointment_time, '%H:%i') AS appointmentTime, a.target_procedure AS targetProcedure, a.status, a.is_reminder_sent AS isReminderSent, a.reminder_sent_at AS reminderSentAt FROM appointments a JOIN donors d ON a.donor_id = d.id LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1 WHERE a.appointment_date >= :weekDate ORDER BY a.appointment_date ASC, a.appointment_time ASC");
        $stmt->execute(['weekDate' => $weekDate]);
        $appointments = $stmt->fetchAll();

        foreach ($appointments as &$app) {
            $app['isReminderSent'] = (bool)$app['isReminderSent'];
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

        if ($appointmentId <= 0) {
            sendJsonResponse("error", "Invalid appointment ID", null, 400);
        }

        try {
            $stmt = $conn->prepare("UPDATE appointments SET is_reminder_sent = 1, reminder_sent_at = NOW(), reminder_sent_by_user_id = :userId WHERE id = :id");
            $stmt->execute(['userId' => $userId, 'id' => $appointmentId]);

            sendJsonResponse("success", "Estado de recordatorio actualizado correctamente", [
                "appointmentId" => $appointmentId,
                "isReminderSent" => true
            ]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else {
        sendJsonResponse("error", "Invalid action", null, 400);
    }
}
?>