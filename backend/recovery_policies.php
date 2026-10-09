<?php
require_once 'db_config.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        $stmt = $conn->query("SELECT id, procedure_type AS procedureType, recovery_days AS recoveryDays, recovery_days_female AS recoveryDaysFemale, max_per_year AS maxPerYear, max_per_year_female AS maxPerYearFemale FROM donation_recovery_policies ORDER BY procedure_type ASC");
        $policies = $stmt->fetchAll();
        sendJsonResponse("success", "Políticas de recuperación recuperadas", $policies);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data) {
        sendJsonResponse("error", "Invalid JSON payload", null, 400);
    }

    $procedureType = strtoupper(trim($data->procedureType ?? $data->procedure_type ?? ''));
    $recoveryDays = (int)($data->recoveryDays ?? $data->recovery_days ?? 0);
    $recoveryDaysFemale = (int)($data->recoveryDaysFemale ?? $data->recovery_days_female ?? $recoveryDays);
    $maxPerYear = (int)($data->maxPerYear ?? $data->max_per_year ?? 0);
    $maxPerYearFemale = (int)($data->maxPerYearFemale ?? $data->max_per_year_female ?? $maxPerYear);

    if (!in_array($procedureType, ['PLASMA', 'PLATELETS', 'BLOOD'])) {
        sendJsonResponse("error", "Tipo de procedimiento inválido", null, 400);
    }
    if ($recoveryDays < 0 || $recoveryDaysFemale < 0 || $maxPerYear < 0 || $maxPerYearFemale < 0) {
        sendJsonResponse("error", "Los valores no pueden ser negativos", null, 400);
    }

    try {
        if ($action === 'create') {
            $stmt = $conn->prepare("INSERT INTO donation_recovery_policies (procedure_type, recovery_days, recovery_days_female, max_per_year, max_per_year_female) VALUES (:procedureType, :recoveryDays, :recoveryDaysFemale, :maxPerYear, :maxPerYearFemale)");
            $stmt->execute([
                'procedureType' => $procedureType,
                'recoveryDays' => $recoveryDays,
                'recoveryDaysFemale' => $recoveryDaysFemale,
                'maxPerYear' => $maxPerYear,
                'maxPerYearFemale' => $maxPerYearFemale
            ]);
            $id = (int)$conn->lastInsertId();
            sendJsonResponse("success", "Política de recuperación creada correctamente", ["id" => $id, "procedureType" => $procedureType]);
        } else if ($action === 'update') {
            $id = (int)($data->id ?? 0);
            if ($id <= 0) {
                sendJsonResponse("error", "ID de política inválido", null, 400);
            }
            $stmt = $conn->prepare("UPDATE donation_recovery_policies SET procedure_type = :procedureType, recovery_days = :recoveryDays, recovery_days_female = :recoveryDaysFemale, max_per_year = :maxPerYear, max_per_year_female = :maxPerYearFemale WHERE id = :id");
            $stmt->execute([
                'id' => $id,
                'procedureType' => $procedureType,
                'recoveryDays' => $recoveryDays,
                'recoveryDaysFemale' => $recoveryDaysFemale,
                'maxPerYear' => $maxPerYear,
                'maxPerYearFemale' => $maxPerYearFemale
            ]);
            sendJsonResponse("success", "Política de recuperación actualizada correctamente", ["id" => $id]);
        } else if ($action === 'delete') {
            $id = (int)($data->id ?? 0);
            if ($id <= 0) {
                sendJsonResponse("error", "ID de política inválido", null, 400);
            }
            $stmt = $conn->prepare("DELETE FROM donation_recovery_policies WHERE id = :id");
            $stmt->execute(['id' => $id]);
            sendJsonResponse("success", "Política de recuperación eliminada correctamente", ["id" => $id]);
        } else {
            sendJsonResponse("error", "Invalid action", null, 400);
        }
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else {
    sendJsonResponse("error", "Invalid request method", null, 405);
}
?>