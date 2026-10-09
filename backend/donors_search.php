<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $dni = trim($_GET['dni'] ?? '');

    if (empty($dni)) {
        sendJsonResponse("error", "DNI parameter is required", null, 400);
    }

    try {
        $like = '%' . $dni . '%';
        $stmt = $conn->prepare("SELECT id, dni, first_name AS firstName, last_name AS lastName, status, next_eligible_date AS nextEligibleDate FROM donors WHERE dni LIKE :dni ORDER BY last_name ASC, first_name ASC LIMIT 50");
        $stmt->execute(['dni' => $like]);
        $donors = $stmt->fetchAll();

        foreach ($donors as &$donor) {
            $isRecoveryVeda = !empty($donor['nextEligibleDate']) && $donor['nextEligibleDate'] > date('Y-m-d');
            if ($isRecoveryVeda) {
                $donor['status'] = 'EN_VEDA';
            }
        }
        unset($donor);

        sendJsonResponse("success", "Donantes encontrados", $donors);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else {
    sendJsonResponse("error", "Invalid request method", null, 405);
}
?>