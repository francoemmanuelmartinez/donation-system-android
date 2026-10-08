<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data) {
        sendJsonResponse("error", "Invalid JSON payload", null, 400);
    }

    $donorId = (int)($data->donorId ?? $data->donor_id ?? 0);
    $weight = (float)($data->weight ?? 0);
    $height = (float)($data->height ?? 0);
    $hto = (float)($data->hto ?? 0);
    $plateletsCount = (int)($data->plateletsCount ?? $data->platelets_count ?? 0);
    $staffUserId = isset($data->staffUserId) ? (int)$data->staffUserId : null;
    $procedures = $data->procedures ?? $data->procedureTypes ?? [];

    if ($donorId <= 0 || $weight <= 0 || $hto <= 0 || $plateletsCount <= 0 || empty($procedures)) {
        sendJsonResponse("error", "Por favor ingrese todos los parámetros clínicos requeridos", null, 400);
    }

    try {
        $conn->beginTransaction();

        $stmt = $conn->prepare("INSERT INTO donations (donor_id, weight, height, hto, platelets_count, staff_user_id) VALUES (:donorId, :weight, :height, :hto, :plateletsCount, :staffUserId)");
        $stmt->execute([
            'donorId' => $donorId,
            'weight' => $weight,
            'height' => $height,
            'hto' => $hto,
            'plateletsCount' => $plateletsCount,
            'staffUserId' => $staffUserId
        ]);

        $donationId = (int)$conn->lastInsertId();

        $procStmt = $conn->prepare("INSERT INTO donation_procedures (donation_id, procedure_type) VALUES (:donationId, :procType)");
        foreach ($procedures as $proc) {
            $procType = strtoupper(trim((string)$proc));
            if (in_array($procType, ['PLASMA', 'PLATELETS', 'BLOOD'])) {
                $procStmt->execute(['donationId' => $donationId, 'procType' => $procType]);
            }
        }

        // Update donor last_donation_date
        $updateDonorStmt = $conn->prepare("UPDATE donors SET last_donation_date = CURRENT_DATE() WHERE id = :donorId");
        $updateDonorStmt->execute(['donorId' => $donorId]);

        $conn->commit();

        sendJsonResponse("success", "Donación registrada correctamente", ["donationId" => $donationId]);
    } catch(PDOException $e) {
        if ($conn->inTransaction()) {
            $conn->rollBack();
        }
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else {
    sendJsonResponse("error", "Invalid request method", null, 405);
}
?>