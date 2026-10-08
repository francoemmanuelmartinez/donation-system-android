<?php
require_once 'db_config.php';

function calculateEligibleWorkdayReturnDate($startDateStr, $vedaDays) {
    $date = new DateTime($startDateStr);
    $date->modify("+$vedaDays days");

    // N: 1 (Mon) through 7 (Sun)
    $dayOfWeek = (int)$date->format('N');
    if ($dayOfWeek === 6) { // Saturday -> advance 2 days to Monday
        $date->modify("+2 days");
    } else if ($dayOfWeek === 7) { // Sunday -> advance 1 day to Monday
        $date->modify("+1 day");
    }
    return $date->format('Y-m-d');
}

$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        $stmt = $conn->query("SELECT id, name, default_veda_days AS defaultVedaDays FROM restrictions ORDER BY name ASC");
        $restrictions = $stmt->fetchAll();
        sendJsonResponse("success", "Diccionario de exclusiones recuperado", $restrictions);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data) {
        sendJsonResponse("error", "Invalid JSON payload", null, 400);
    }

    if ($action === 'create') {
        $name = trim($data->name ?? '');
        $defaultVedaDays = (int)($data->defaultVedaDays ?? $data->default_veda_days ?? 0);

        if (empty($name) || $defaultVedaDays < 0) {
            sendJsonResponse("error", "Por favor ingrese un nombre válido y días de veda mayores o iguales a 0", null, 400);
        }

        try {
            $stmt = $conn->prepare("INSERT INTO restrictions (name, default_veda_days) VALUES (:name, :vedaDays)");
            $stmt->execute(['name' => $name, 'vedaDays' => $defaultVedaDays]);
            $id = (int)$conn->lastInsertId();

            sendJsonResponse("success", "Tipo de exclusión creada correctamente", ["id" => $id, "name" => $name, "defaultVedaDays" => $defaultVedaDays]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else if ($action === 'assign') {
        $donorId = (int)($data->donorId ?? $data->donor_id ?? 0);
        $restrictionId = (int)($data->restrictionId ?? $data->restriction_id ?? 0);
        $vedaDays = (int)($data->vedaDays ?? $data->veda_days ?? 0);
        $notes = trim($data->notes ?? '');
        $createdByUserId = isset($data->createdByUserId) ? (int)$data->createdByUserId : null;
        $startDateStr = date('Y-m-d');

        if ($donorId <= 0 || $restrictionId <= 0 || $vedaDays < 0) {
            sendJsonResponse("error", "Parámetros de exclusión inválidos", null, 400);
        }

        $calculatedEndDateObj = new DateTime($startDateStr);
        $calculatedEndDateObj->modify("+$vedaDays days");
        $calculatedEndDateStr = $calculatedEndDateObj->format('Y-m-d');

        $eligibleWorkdayReturnDateStr = calculateEligibleWorkdayReturnDate($startDateStr, $vedaDays);

        try {
            $conn->beginTransaction();

            $stmt = $conn->prepare("INSERT INTO donor_restrictions (donor_id, restriction_id, start_date, veda_days, calculated_end_date, eligible_workday_return_date, notes, created_by_user_id) VALUES (:donorId, :restrictionId, :startDate, :vedaDays, :calculatedEndDate, :eligibleWorkday, :notes, :createdUserId)");
            $stmt->execute([
                'donorId' => $donorId,
                'restrictionId' => $restrictionId,
                'startDate' => $startDateStr,
                'vedaDays' => $vedaDays,
                'calculatedEndDate' => $calculatedEndDateStr,
                'eligibleWorkday' => $eligibleWorkdayReturnDateStr,
                'notes' => $notes,
                'createdUserId' => $createdByUserId
            ]);

            $updateDonorStmt = $conn->prepare("UPDATE donors SET status = 'EN_VEDA' WHERE id = :donorId");
            $updateDonorStmt->execute(['donorId' => $donorId]);

            $conn->commit();

            sendJsonResponse("success", "Exclusión médica asignada al paciente", [
                "donorId" => $donorId,
                "startDate" => $startDateStr,
                "vedaDays" => $vedaDays,
                "eligibleWorkdayReturnDate" => $eligibleWorkdayReturnDateStr
            ]);
        } catch(PDOException $e) {
            if ($conn->inTransaction()) {
                $conn->rollBack();
            }
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else {
        sendJsonResponse("error", "Invalid action", null, 400);
    }
}
?>