<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $bloodType = trim($_GET['blood_type'] ?? $_GET['bloodType'] ?? '');

    if (empty($bloodType)) {
        sendJsonResponse("error", "Blood type parameter is required", null, 400);
    }

    try {
        $stmt = $conn->prepare("SELECT d.id, d.dni, d.first_name AS firstName, d.last_name AS lastName, d.blood_type AS bloodType, d.phenotype, d.status, d.last_donation_date AS lastDonationDate, dp.phone_number AS phoneNumber FROM donors d LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1 WHERE d.blood_type = :bloodType AND d.status = 'APTO' ORDER BY d.last_donation_date ASC, d.last_name ASC");
        $stmt->execute(['bloodType' => $bloodType]);
        $donors = $stmt->fetchAll();

        sendJsonResponse("success", "Donantes aptos encontrados", $donors);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else {
    sendJsonResponse("error", "Invalid request method", null, 405);
}
?>