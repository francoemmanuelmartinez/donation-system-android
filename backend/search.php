<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $bloodType = trim($_GET['blood_type'] ?? $_GET['bloodType'] ?? '');

    try {
        $sql = "SELECT d.id, d.dni, d.first_name AS firstName, d.last_name AS lastName, d.gender, d.blood_type AS bloodType, d.phenotype, d.last_donation_date AS lastDonationDate, d.next_eligible_date AS nextEligibleDate, dp.phone_number AS phoneNumber FROM donors d LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1";
        $params = [];
        if (!empty($bloodType)) {
            $sql .= " WHERE d.blood_type = :bloodType";
            $params['bloodType'] = $bloodType;
        }

        $stmt = $conn->prepare($sql);
        $stmt->execute($params);
        $donors = $stmt->fetchAll();

        // Active medical restrictions (furthest returning date per donor)
        $restrStmt = $conn->query("SELECT dr.donor_id AS donorId, dr.eligible_workday_return_date AS eligibleWorkdayReturnDate, r.name AS restrictionName FROM donor_restrictions dr JOIN restrictions r ON dr.restriction_id = r.id WHERE dr.eligible_workday_return_date >= CURRENT_DATE() ORDER BY dr.eligible_workday_return_date DESC");
        $restrictionsByDonor = [];
        while ($r = $restrStmt->fetch()) {
            $did = (int)$r['donorId'];
            if (!isset($restrictionsByDonor[$did])) {
                $restrictionsByDonor[$did] = $r;
            }
        }

        $today = date('Y-m-d');
        foreach ($donors as &$donor) {
            $recoveryEnd = (!empty($donor['nextEligibleDate']) && $donor['nextEligibleDate'] > $today) ? $donor['nextEligibleDate'] : null;
            $led = isset($restrictionsByDonor[(int)$donor['id']]) ? $restrictionsByDonor[(int)$donor['id']] : null;
            $restrictionEnd = $led ? $led['eligibleWorkdayReturnDate'] : null;
            $restrictionReason = $led ? $led['restrictionName'] : null;

            if ($recoveryEnd !== null && $restrictionEnd !== null) {
                if ($recoveryEnd >= $restrictionEnd) {
                    $state = 'VEDA_RECUPERACION';
                    $vedaEndDate = $recoveryEnd;
                    $vedaReason = 'Recuperación post-donación';
                } else {
                    $state = 'VEDA_RESTRICCION';
                    $vedaEndDate = $restrictionEnd;
                    $vedaReason = $restrictionReason;
                }
            } else if ($recoveryEnd !== null) {
                $state = 'VEDA_RECUPERACION';
                $vedaEndDate = $recoveryEnd;
                $vedaReason = 'Recuperación post-donación';
            } else if ($restrictionEnd !== null) {
                $state = 'VEDA_RESTRICCION';
                $vedaEndDate = $restrictionEnd;
                $vedaReason = $restrictionReason;
            } else {
                $state = 'APTO';
                $vedaEndDate = null;
                $vedaReason = null;
            }

            $donor['state'] = $state;
            $donor['status'] = ($state === 'APTO') ? 'APTO' : 'EN_VEDA';
            $donor['vedaEndDate'] = $vedaEndDate;
            $donor['vedaReason'] = $vedaReason;
            $donor['recoveryVedaEndDate'] = $recoveryEnd;
            $donor['restrictionVedaEndDate'] = $restrictionEnd;
            $donor['restrictionVedaReason'] = $restrictionReason;
        }
        unset($donor);

        // Apto donors first (last donation oldest first, never donated first),
        // then veda donors by end date ascending (closest first).
        usort($donors, function ($a, $b) {
            $aApto = ($a['state'] === 'APTO') ? 1 : 0;
            $bApto = ($b['state'] === 'APTO') ? 1 : 0;
            if ($aApto !== $bApto) {
                return $bApto - $aApto;
            }
            if ($aApto === 1) {
                $aDate = $a['lastDonationDate'] ?? '0000-00-00';
                $bDate = $b['lastDonationDate'] ?? '0000-00-00';
                return strcmp($aDate, $bDate);
            }
            $aDate = $a['vedaEndDate'] ?? '9999-12-31';
            $bDate = $b['vedaEndDate'] ?? '9999-12-31';
            return strcmp($aDate, $bDate);
        });

        sendJsonResponse("success", "Donantes encontrados", $donors);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else {
    sendJsonResponse("error", "Invalid request method", null, 405);
}
?>