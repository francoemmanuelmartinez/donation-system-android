<?php
require_once 'db_config.php';

function annualDonationCount($conn, $donorId, $procType) {
    $stmt = $conn->prepare("SELECT COUNT(*) AS c FROM donations d JOIN donation_procedures dp ON dp.donation_id = d.id WHERE d.donor_id = :donorId AND dp.procedure_type = :procType AND d.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 365 DAY)");
    $stmt->execute(['donorId' => $donorId, 'procType' => $procType]);
    return (int)$stmt->fetch()['c'];
}

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

    if ($donorId <= 0 || $hto <= 0 || $plateletsCount <= 0 || empty($procedures)) {
        sendJsonResponse("error", "Por favor ingrese todos los parámetros clínicos requeridos", null, 400);
    }

    try {
        $conn->beginTransaction();

        // Donor gender drives female recovery limits
        $donorStmt = $conn->prepare("SELECT gender FROM donors WHERE id = :donorId");
        $donorStmt->execute(['donorId' => $donorId]);
        $donorData = $donorStmt->fetch();
        if (!$donorData) {
            sendJsonResponse("error", "Donante no encontrado", null, 404);
        }
        $isFemale = ($donorData['gender'] === 'Femenino');

        // Recovery policies per procedure type
        $policyStmt = $conn->query("SELECT procedure_type AS procedureType, recovery_days AS recoveryDays, recovery_days_female AS recoveryDaysFemale, max_per_year AS maxPerYear, max_per_year_female AS maxPerYearFemale FROM donation_recovery_policies");
        $policies = [];
        while ($row = $policyStmt->fetch()) {
            $policies[$row['procedureType']] = $row;
        }

        // Validate that recovery policy exists for each requested procedure
        foreach ($procedures as $proc) {
            $procType = strtoupper(trim((string)$proc));
            if (!isset($policies[$procType])) {
                $conn->rollBack();
                sendJsonResponse("error", "No existe política de recuperación para el procedimiento $procType", null, 400);
            }
        }

        $stmt = $conn->prepare("INSERT INTO donations (donor_id, weight, height, hto, platelets_count, annual_limit_override, staff_user_id) VALUES (:donorId, :weight, :height, :hto, :plateletsCount, 0, :staffUserId)");
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
        $nextEligibleDate = null;
        $warnings = [];
        foreach ($procedures as $proc) {
            $procType = strtoupper(trim((string)$proc));
            $procStmt->execute(['donationId' => $donationId, 'procType' => $procType]);

            $policy = $policies[$procType];
            $recoveryDays = $isFemale ? (int)$policy['recoveryDaysFemale'] : (int)$policy['recoveryDays'];
            $candidate = calculateEligibleWorkdayReturnDate(date('Y-m-d'), $recoveryDays);
            if ($nextEligibleDate === null || $candidate > $nextEligibleDate) {
                $nextEligibleDate = $candidate;
            }
        }

        // Annual count per procedure (rolling 365 days, includes the current donation)
        $annualCounts = [];
        foreach ($policies as $procType => $policy) {
            $count = annualDonationCount($conn, $donorId, $procType);
            $maxPerYear = $isFemale ? (int)$policy['maxPerYearFemale'] : (int)$policy['maxPerYear'];
            $annualCounts[$procType] = ["count" => $count, "max" => $maxPerYear];
            if ($count > $maxPerYear) {
                $warnings[] = "El donante supera el límite anual de $procType ($count/$maxPerYear)";
            }
        }

        if (!empty($warnings)) {
            $overrideStmt = $conn->prepare("UPDATE donations SET annual_limit_override = 1 WHERE id = :donationId");
            $overrideStmt->execute(['donationId' => $donationId]);
        }

        // Update donor last donation + next eligible date
        $updateDonorStmt = $conn->prepare("UPDATE donors SET last_donation_date = CURRENT_DATE(), next_eligible_date = :nextEligible WHERE id = :donorId");
        $updateDonorStmt->execute(['donorId' => $donorId, 'nextEligible' => $nextEligibleDate]);

        $conn->commit();

        sendJsonResponse("success", "Donación registrada correctamente", [
            "donationId" => $donationId,
            "nextEligibleDate" => $nextEligibleDate,
            "annualCounts" => $annualCounts,
            "warnings" => $warnings
        ]);
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