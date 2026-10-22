<?php
require_once 'db_config.php';
require_once 'appointments_lib.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

/**
 * Restricción médica activa: la veda permanente nunca vence; la temporal se
 * activa mientras su fecha de retorno hábil no haya pasado.
 */
function restrictIsActiveSql($alias = 'dr') {
    return "($alias.is_permanent = 1 OR $alias.eligible_workday_return_date >= CURRENT_DATE())";
}

/** Deriva donors status a partir de las restricciones activas y la recuperación (sin mutar). */
function deriveDonorStatus($conn, $donorId) {
    $activeStmt = $conn->prepare("SELECT 1 FROM donor_restrictions WHERE donor_id = :donorId AND " . restrictIsActiveSql('donor_restrictions') . " LIMIT 1");
    $activeStmt->execute(['donorId' => $donorId]);
    $hasActiveRestriction = (bool)$activeStmt->fetch();

    $nextStmt = $conn->prepare("SELECT next_eligible_date FROM donors WHERE id = :id");
    $nextStmt->execute(['id' => $donorId]);
    $nextEligible = $nextStmt->fetchColumn();
    $inRecovery = $nextEligible !== null && $nextEligible !== false && $nextEligible > date('Y-m-d');

    return ($hasActiveRestriction || $inRecovery) ? 'EN_VEDA' : 'APTO';
}

/** Normaliza un elemento de la lista de restricciones del request. */
function normalizeRestrictionEntry($raw) {
    return [
        'restrictionId' => (int)($raw->restrictionId ?? $raw->restriction_id ?? 0),
        'vedaDays'      => isset($raw->vedaDays) ? (int)$raw->vedaDays
                          : (isset($raw->veda_days) ? (int)$raw->veda_days : 0),
        'isPermanent'   => !empty($raw->isPermanent) || !empty($raw->is_permanent),
        'notes'         => trim($raw->notes ?? ''),
        'startDate'     => trim($raw->startDate ?? $raw->start_date ?? ''),
    ];
}

/** Inserta una asignación de restricción y devuelve la fila calculada. */
function insertRestriction($conn, $donorId, $entry, $createdByUserId, $centerId = 1) {
    $startDate = $entry['startDate'] !== '' ? $entry['startDate'] : date('Y-m-d');

    if ($entry['isPermanent']) {
        $stmt = $conn->prepare("INSERT INTO donor_restrictions (donor_id, restriction_id, start_date, veda_days, is_permanent, calculated_end_date, eligible_workday_return_date, notes, created_by_user_id, center_id) VALUES (:donorId, :restrictionId, :startDate, NULL, 1, NULL, NULL, :notes, :createdUserId, :centerId)");
        $stmt->execute([
            'donorId'       => $donorId,
            'restrictionId' => $entry['restrictionId'],
            'startDate'     => $startDate,
            'notes'         => $entry['notes'],
            'createdUserId' => $createdByUserId,
            'centerId'      => $centerId,
        ]);

        return [
            'id'                       => (int)$conn->lastInsertId(),
            'donorId'                  => $donorId,
            'restrictionId'            => $entry['restrictionId'],
            'startDate'                => $startDate,
            'vedaDays'                 => null,
            'isPermanent'              => true,
            'calculatedEndDate'        => null,
            'eligibleWorkdayReturnDate'=> null,
            'notes'                    => $entry['notes'],
        ];
    }

    $calculatedEndDate = (new DateTime($startDate))->modify('+' . $entry['vedaDays'] . ' days')->format('Y-m-d');
    $eligibleWorkday = calculateEligibleWorkdayReturnDate($startDate, $entry['vedaDays']);

    $stmt = $conn->prepare("INSERT INTO donor_restrictions (donor_id, restriction_id, start_date, veda_days, is_permanent, calculated_end_date, eligible_workday_return_date, notes, created_by_user_id, center_id) VALUES (:donorId, :restrictionId, :startDate, :vedaDays, 0, :calculatedEndDate, :eligibleWorkday, :notes, :createdUserId, :centerId)");
    $stmt->execute([
        'donorId'            => $donorId,
        'restrictionId'      => $entry['restrictionId'],
        'startDate'          => $startDate,
        'vedaDays'           => $entry['vedaDays'],
        'calculatedEndDate'  => $calculatedEndDate,
        'eligibleWorkday'    => $eligibleWorkday,
        'notes'              => $entry['notes'],
        'createdUserId'      => $createdByUserId,
        'centerId'           => $centerId,
    ]);

    return [
        'id'                       => (int)$conn->lastInsertId(),
        'donorId'                  => $donorId,
        'restrictionId'            => $entry['restrictionId'],
        'startDate'                => $startDate,
        'vedaDays'                 => $entry['vedaDays'],
        'isPermanent'              => false,
        'calculatedEndDate'        => $calculatedEndDate,
        'eligibleWorkdayReturnDate'=> $eligibleWorkday,
        'notes'                    => $entry['notes'],
    ];
}

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
        $createdByUserId = isset($data->createdByUserId) ? (int)$data->createdByUserId
            : (isset($data->created_by_user_id) ? (int)$data->created_by_user_id : null);
        $reschedule = $data->reschedule ?? null;

        if ($donorId <= 0) {
            sendJsonResponse("error", "Parámetros de exclusión inválidos", null, 400);
        }

        $entries = [];
        if (isset($data->restrictions) && is_array($data->restrictions)) {
            foreach ($data->restrictions as $raw) {
                $entries[] = normalizeRestrictionEntry($raw);
            }
        } else {
            $entries[] = normalizeRestrictionEntry($data);
        }

        if (empty($entries)) {
            sendJsonResponse("error", "Debés indicar al menos una restricción", null, 400);
        }
        foreach ($entries as $entry) {
            if ($entry['restrictionId'] <= 0) {
                sendJsonResponse("error", "Parámetros de exclusión inválidos", null, 400);
            }
            if (!$entry['isPermanent'] && $entry['vedaDays'] < 0) {
                sendJsonResponse("error", "Parámetros de exclusión inválidos", null, 400);
            }
        }

        try {
            $existsStmt = $conn->prepare("SELECT id, center_id FROM donors WHERE id = :id");
            $existsStmt->execute(['id' => $donorId]);
            $donorRow = $existsStmt->fetch();
            if (!$donorRow) {
                sendJsonResponse("error", "No se encontró el donante seleccionado", null, 404);
            }
            $centerId = (int)($donorRow['center_id'] ?? 0);
            if ($centerId <= 0) { $centerId = 1; }

            $conn->beginTransaction();

            $assigned = [];
            foreach ($entries as $entry) {
                $assigned[] = insertRestriction($conn, $donorId, $entry, $createdByUserId, $centerId);
            }

            $appointmentInfo = [];
            if ($reschedule) {
                $rescheduleResult = handleReschedule($conn, $donorId, $reschedule);
                if (isset($rescheduleResult['error'])) {
                    $conn->rollBack();
                    sendJsonResponse("error", $rescheduleResult['error'], null, $rescheduleResult['status'] ?? 400);
                }
                $appointmentInfo = $rescheduleResult['appointment'];
            }

            $conn->commit();

            sendJsonResponse("success", "Exclusión médica asignada al donante", [
                "donorId"     => $donorId,
                "restrictions"=> $assigned,
                "appointment" => $appointmentInfo,
            ]);
        } catch(PDOException $e) {
            if ($conn->inTransaction()) {
                $conn->rollBack();
            }
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }

    } else if ($action === 'update') {
        $donorRestrictionId = (int)($data->donorRestrictionId ?? $data->donor_restriction_id ?? 0);

        if ($donorRestrictionId <= 0) {
            sendJsonResponse("error", "Parámetros de exclusión inválidos", null, 400);
        }

        try {
            $stmt = $conn->prepare("SELECT id, donor_id, restriction_id, start_date, veda_days, is_permanent, notes, eligible_workday_return_date FROM donor_restrictions WHERE id = :id");
            $stmt->execute(['id' => $donorRestrictionId]);
            $existing = $stmt->fetch();
            if (!$existing) {
                sendJsonResponse("error", "Exclusión médica no encontrada", null, 404);
            }

            $donorId       = (int)$existing['donor_id'];
            $restrictionId = isset($data->restrictionId) ? (int)$data->restrictionId
                : (isset($data->restriction_id) ? (int)$data->restriction_id : (int)$existing['restriction_id']);
            $isPermanent   = isset($data->isPermanent) || isset($data->is_permanent)
                ? (!empty($data->isPermanent) || !empty($data->is_permanent))
                : ((int)$existing['is_permanent'] === 1);
            $vedaDays      = isset($data->vedaDays) ? (int)$data->vedaDays
                : (isset($data->veda_days) ? (int)$data->veda_days
                    : (int)$existing['veda_days']);
            $notes         = isset($data->notes) ? trim($data->notes) : $existing['notes'];
            $startDate     = trim($data->startDate ?? $data->start_date ?? $existing['start_date']);
            $reschedule    = $data->reschedule ?? null;

            if ($restrictionId <= 0 || (!$isPermanent && $vedaDays < 0)) {
                sendJsonResponse("error", "Parámetros de exclusión inválidos", null, 400);
            }

            $conn->beginTransaction();

            if ($isPermanent) {
                $conn->prepare("UPDATE donor_restrictions SET restriction_id = :restrictionId, start_date = :startDate, veda_days = NULL, is_permanent = 1, calculated_end_date = NULL, eligible_workday_return_date = NULL, notes = :notes WHERE id = :id")
                     ->execute([
                         'restrictionId' => $restrictionId,
                         'startDate'     => $startDate,
                         'notes'         => $notes,
                         'id'            => $donorRestrictionId,
                     ]);
                $eligibleWorkday = null;
            } else {
                $calculatedEndDate = (new DateTime($startDate))->modify('+' . $vedaDays . ' days')->format('Y-m-d');
                $eligibleWorkday   = calculateEligibleWorkdayReturnDate($startDate, $vedaDays);
                $conn->prepare("UPDATE donor_restrictions SET restriction_id = :restrictionId, start_date = :startDate, veda_days = :vedaDays, is_permanent = 0, calculated_end_date = :calculatedEndDate, eligible_workday_return_date = :eligibleWorkday, notes = :notes WHERE id = :id")
                     ->execute([
                         'restrictionId'     => $restrictionId,
                         'startDate'         => $startDate,
                         'vedaDays'          => $vedaDays,
                         'calculatedEndDate' => $calculatedEndDate,
                         'eligibleWorkday'   => $eligibleWorkday,
                         'notes'             => $notes,
                         'id'                => $donorRestrictionId,
                     ]);
            }

            $status = deriveDonorStatus($conn, $donorId);

            $appointmentInfo = [];
            if ($reschedule) {
                $rescheduleResult = handleReschedule($conn, $donorId, $reschedule);
                if (isset($rescheduleResult['error'])) {
                    $conn->rollBack();
                    sendJsonResponse("error", $rescheduleResult['error'], null, $rescheduleResult['status'] ?? 400);
                }
                $appointmentInfo = $rescheduleResult['appointment'];
            }

            $conn->commit();

            sendJsonResponse("success", "Exclusión médica actualizada", [
                "donorRestrictionId"       => $donorRestrictionId,
                "donorId"                  => $donorId,
                "vedaDays"                 => $isPermanent ? null : $vedaDays,
                "isPermanent"              => $isPermanent,
                "eligibleWorkdayReturnDate"=> $eligibleWorkday,
                "status"                   => $status,
                "appointment"              => $appointmentInfo,
            ]);
        } catch(PDOException $e) {
            if ($conn->inTransaction()) {
                $conn->rollBack();
            }
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }

    } else if ($action === 'revoke') {
        $donorRestrictionId = (int)($data->donorRestrictionId ?? $data->donor_restriction_id ?? 0);
        $donorId            = (int)($data->donorId ?? $data->donor_id ?? 0);

        if ($donorRestrictionId <= 0 && $donorId <= 0) {
            sendJsonResponse("error", "Indicá la exclusión o el donante a actualizar", null, 400);
        }

        try {
            $conn->beginTransaction();

            if ($donorRestrictionId > 0) {
                $stmt = $conn->prepare("SELECT donor_id FROM donor_restrictions WHERE id = :id");
                $stmt->execute(['id' => $donorRestrictionId]);
                $row = $stmt->fetch();
                if (!$row) {
                    $conn->rollBack();
                    sendJsonResponse("error", "Exclusión médica no encontrada", null, 404);
                }
                $donorId = (int)$row['donor_id'];
                $conn->prepare("DELETE FROM donor_restrictions WHERE id = :id")
                     ->execute(['id' => $donorRestrictionId]);
            } else {
                $stmt = $conn->prepare("SELECT COUNT(*) FROM donor_restrictions WHERE donor_id = :donorId");
                $stmt->execute(['donorId' => $donorId]);
                if ((int)$stmt->fetchColumn() === 0) {
                    $conn->rollBack();
                    sendJsonResponse("error", "No hay exclusiones médicas para ese donante", null, 404);
                }
                $conn->prepare("DELETE FROM donor_restrictions WHERE donor_id = :donorId")
                     ->execute(['donorId' => $donorId]);
            }

            $status = deriveDonorStatus($conn, $donorId);

            $conn->commit();

            sendJsonResponse("success", "Exclusión médica revocada", [
                "donorRestrictionId" => $donorRestrictionId > 0 ? $donorRestrictionId : null,
                "donorId"            => $donorId,
                "status"             => $status,
            ]);
        } catch(PDOException $e) {
            if ($conn->inTransaction()) {
                $conn->rollBack();
            }
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }

    } else if ($action === 'delete') {
        $restrictionId = (int)($data->restrictionId ?? $data->restriction_id ?? 0);

        if ($restrictionId <= 0) {
            sendJsonResponse("error", "ID de exclusión inválido", null, 400);
        }

        try {
            $checkStmt = $conn->prepare("SELECT id FROM restrictions WHERE id = :id");
            $checkStmt->execute(['id' => $restrictionId]);
            if (!$checkStmt->fetch()) {
                sendJsonResponse("error", "Tipo de exclusión no encontrado", null, 404);
            }

            $deleteStmt = $conn->prepare("DELETE FROM restrictions WHERE id = :id");
            $deleteStmt->execute(['id' => $restrictionId]);

            sendJsonResponse("success", "Tipo de exclusión eliminado correctamente", ["restrictionId" => $restrictionId]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else {
        sendJsonResponse("error", "Invalid action", null, 400);
    }
}
?>
