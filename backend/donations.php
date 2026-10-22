<?php
require_once 'db_config.php';
require_once 'appointments_lib.php';

function annualDonationCount($conn, $donorId, $procType) {
    $stmt = $conn->prepare("SELECT COUNT(*) AS c FROM donations d JOIN donation_procedures dp ON dp.donation_id = d.id WHERE d.donor_id = :donorId AND dp.procedure_type = :procType AND d.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 365 DAY)");
    $stmt->execute(['donorId' => $donorId, 'procType' => $procType]);
    return (int)$stmt->fetch()['c'];
}

// GET: donation history (with "atendido por")
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $donorId = (int)($_GET['donorId'] ?? $_GET['donor_id'] ?? 0);
    $staffUserId = (int)($_GET['staffUserId'] ?? $_GET['staff_user_id'] ?? 0);
    $centerId = (int)($_GET['centerId'] ?? $_GET['center_id'] ?? 0);
    $limit = (int)($_GET['limit'] ?? 50);

    try {
        $where = [];
        $params = [];

        if ($donorId > 0) {
            $where[] = "d.donor_id = :donorId";
            $params['donorId'] = $donorId;
        }
        if ($staffUserId > 0) {
            $where[] = "d.staff_user_id = :staffUserId";
            $params['staffUserId'] = $staffUserId;
        }
        if ($centerId > 0) {
            $where[] = "d.center_id = :centerId";
            $params['centerId'] = $centerId;
        }

        $sql = "SELECT d.id, d.donor_id AS donorId, CONCAT(dn.first_name, ' ', dn.last_name) AS donorName,
                       d.weight, d.height, d.hto, d.platelets_count AS plateletsCount,
                       d.annual_limit_override AS annualLimitOverride,
                       d.staff_user_id AS staffUserId,
                       CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) AS staffName,
                       d.created_at AS createdAt,
                       (SELECT GROUP_CONCAT(dp.procedure_type) FROM donation_procedures dp WHERE dp.donation_id = d.id) AS procedures
                FROM donations d
                JOIN donors dn ON dn.id = d.donor_id
                LEFT JOIN users u ON u.id = d.staff_user_id";

        if (!empty($where)) {
            $sql .= " WHERE " . implode(" AND ", $where);
        }
        $sql .= " ORDER BY d.created_at DESC LIMIT :limit";

        $stmt = $conn->prepare($sql);
        foreach ($params as $k => $v) {
            $stmt->bindValue(":$k", $v, PDO::PARAM_INT);
        }
        $stmt->bindValue(":limit", $limit, PDO::PARAM_INT);
        $stmt->execute();
        $rows = $stmt->fetchAll();

        $donations = array_map(function ($r) {
            $r['staffName'] = trim($r['staffName']);
            $r['procedures'] = $r['procedures'] ? explode(',', $r['procedures']) : [];
            $r['staffUserId'] = $r['staffUserId'] !== null ? (int)$r['staffUserId'] : null;
            return $r;
        }, $rows);

        sendJsonResponse("success", "Donaciones recuperadas", $donations);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'POST') {
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
    $centerId = (int)($data->centerId ?? $data->center_id ?? $_GET['centerId'] ?? $_GET['center_id'] ?? 0);
    if ($centerId <= 0) { $centerId = 1; }
    $appointmentId = (int)($data->appointmentId ?? $data->appointment_id ?? 0);
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

        // Optional link to a scheduled appointment: must belong to the donor and be SCHEDULED.
        if ($appointmentId > 0) {
            $apptStmt = $conn->prepare("SELECT id, status FROM appointments WHERE id = :id AND donor_id = :donorId");
            $apptStmt->execute(['id' => $appointmentId, 'donorId' => $donorId]);
            $apptRow = $apptStmt->fetch();
            if (!$apptRow) {
                $conn->rollBack();
                sendJsonResponse("error", "El turno indicado no pertenece a este donante", null, 409);
            }
            if ($apptRow['status'] !== 'SCHEDULED') {
                $conn->rollBack();
                sendJsonResponse("error", "El turno indicado no está agendado", null, 409);
            }
        } else {
            $appointmentId = 0;
        }

        // Recovery policies per procedure type
        $policyStmt = $conn->query("SELECT procedure_type AS procedureType, recovery_days AS recoveryDays, recovery_days_female AS recoveryDaysFemale, max_per_year AS maxPerYear, max_per_year_female AS maxPerYearFemale FROM donation_recovery_policies");
        $policies = [];
        while ($row = $policyStmt->fetch()) {
            $policies[$row['procedureType']] = $row;
        }

        // Ignore requested procedures that have no configured recovery policy.
        $validProcedures = [];
        foreach ((array)$procedures as $proc) {
            $procType = strtoupper(trim((string)$proc));
            if ($procType !== '' && isset($policies[$procType]) && !in_array($procType, $validProcedures, true)) {
                $validProcedures[] = $procType;
            }
        }
        if (empty($validProcedures)) {
            $conn->rollBack();
            sendJsonResponse("error", "No hay procedimientos válidos para registrar", null, 400);
        }
        $procedures = $validProcedures;

        $stmt = $conn->prepare("INSERT INTO donations (donor_id, weight, height, hto, platelets_count, annual_limit_override, staff_user_id, center_id, appointment_id) VALUES (:donorId, :weight, :height, :hto, :plateletsCount, 0, :staffUserId, :centerId, :appointmentId)");
        $stmt->execute([
            'donorId' => $donorId,
            'weight' => $weight,
            'height' => $height,
            'hto' => $hto,
            'plateletsCount' => $plateletsCount,
            'staffUserId' => $staffUserId,
            'centerId' => $centerId,
            'appointmentId' => $appointmentId > 0 ? $appointmentId : null
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

        // Link the fulfilled appointment and mark it ATTENDED (same transaction).
        if ($appointmentId > 0) {
            $markError = markAppointmentAttendance($conn, $appointmentId, $donorId, 'ATTENDED', $staffUserId);
            if ($markError !== null) {
                $conn->rollBack();
                sendJsonResponse("error", $markError, null, 409);
            }
        }

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