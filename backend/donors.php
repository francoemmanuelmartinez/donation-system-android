<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $dni = trim($_GET['dni'] ?? '');
    $id = (int)($_GET['id'] ?? 0);

    if ($id <= 0 && empty($dni)) {
        sendJsonResponse("error", "DNI or ID parameter is required", null, 400);
    }

    try {
        if ($id > 0) {
            $stmt = $conn->prepare("SELECT id, dni, first_name AS firstName, last_name AS lastName, gender, birth_date AS birthDate, city, zip_code AS zipCode, address, neighborhood, blood_type AS bloodType, phenotype, weight, height, status, last_donation_date AS lastDonationDate, next_eligible_date AS nextEligibleDate FROM donors WHERE id = :id LIMIT 1");
            $stmt->execute(['id' => $id]);
        } else {
            $stmt = $conn->prepare("SELECT id, dni, first_name AS firstName, last_name AS lastName, gender, birth_date AS birthDate, city, zip_code AS zipCode, address, neighborhood, blood_type AS bloodType, phenotype, weight, height, status, last_donation_date AS lastDonationDate, next_eligible_date AS nextEligibleDate FROM donors WHERE dni = :dni LIMIT 1");
            $stmt->execute(['dni' => $dni]);
        }
        $donor = $stmt->fetch();

        if (!$donor) {
            sendJsonResponse("error", "Donante no encontrado", null, 404);
        }

        $donorId = (int)$donor['id'];

        // Phones
        $phoneStmt = $conn->prepare("SELECT phone_number AS phoneNumber, phone_type AS phoneType, is_primary AS isPrimary FROM donor_phones WHERE donor_id = :donorId");
        $phoneStmt->execute(['donorId' => $donorId]);
        $donor['phones'] = array_map(function ($p) {
            $p['isPrimary'] = (bool)$p['isPrimary'];
            return $p;
        }, $phoneStmt->fetchAll());

        // Emails
        $emailStmt = $conn->prepare("SELECT email_address AS emailAddress, email_type AS emailType, is_primary AS isPrimary FROM donor_emails WHERE donor_id = :donorId");
        $emailStmt->execute(['donorId' => $donorId]);
        $donor['emails'] = array_map(function ($e) {
            $e['isPrimary'] = (bool)$e['isPrimary'];
            return $e;
        }, $emailStmt->fetchAll());

        // Active Medical Restrictions (la veda permanente nunca vence)
        $restrStmt = $conn->prepare("SELECT dr.id, dr.restriction_id AS restrictionId, r.name AS restrictionName, dr.start_date AS startDate, dr.veda_days AS vedaDays, dr.is_permanent AS isPermanent, dr.calculated_end_date AS calculatedEndDate, dr.eligible_workday_return_date AS eligibleWorkdayReturnDate, dr.notes FROM donor_restrictions dr JOIN restrictions r ON dr.restriction_id = r.id WHERE dr.donor_id = :donorId AND (dr.is_permanent = 1 OR dr.eligible_workday_return_date >= CURRENT_DATE()) ORDER BY dr.is_permanent DESC, dr.eligible_workday_return_date DESC, dr.id DESC");
        $restrStmt->execute(['donorId' => $donorId]);
        $restrictions = array_map(function ($r) {
            $r['isPermanent'] = (bool)$r['isPermanent'];
            return $r;
        }, $restrStmt->fetchAll());

        $donor['activeRestrictions'] = $restrictions;
        $donor['status'] = (!empty($restrictions)) ? 'EN_VEDA' : 'APTO';

        // Recovery veda (post-donation) derived from next eligible date
        $isRecoveryVeda = !empty($donor['nextEligibleDate']) && $donor['nextEligibleDate'] > date('Y-m-d');
        if ($isRecoveryVeda) {
            $donor['status'] = 'EN_VEDA';
        }

        // Annual donation counts per procedure type (rolling 365 days)
        $policyStmt = $conn->query("SELECT procedure_type AS procedureType, max_per_year AS maxPerYear, max_per_year_female AS maxPerYearFemale FROM donation_recovery_policies");
        $annualCounts = [];
        $isFemale = ($donor['gender'] === 'Femenino');
        while ($policy = $policyStmt->fetch()) {
            $countStmt = $conn->prepare("SELECT COUNT(*) AS c FROM donations d JOIN donation_procedures dp ON dp.donation_id = d.id WHERE d.donor_id = :donorId AND dp.procedure_type = :procType AND d.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 365 DAY)");
            $countStmt->execute(['donorId' => $donorId, 'procType' => $policy['procedureType']]);
            $count = (int)$countStmt->fetch()['c'];
            $maxPerYear = $isFemale ? (int)$policy['maxPerYearFemale'] : (int)$policy['maxPerYear'];
            $annualCounts[$policy['procedureType']] = ["count" => $count, "max" => $maxPerYear];
        }
        $donor['annualCounts'] = $annualCounts;

        sendJsonResponse("success", "Donante encontrado", $donor);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data) {
        sendJsonResponse("error", "Invalid JSON payload", null, 400);
    }

    $dni = trim($data->dni ?? '');
    $firstName = trim($data->firstName ?? $data->first_name ?? '');
    $lastName = trim($data->lastName ?? $data->last_name ?? '');
    $gender = trim($data->gender ?? 'Otro');
    $birthDate = trim($data->birthDate ?? $data->birth_date ?? '1990-01-01');
    $city = trim($data->city ?? '');
    $zipCode = trim($data->zipCode ?? $data->zip_code ?? '');
    $address = trim($data->address ?? '');
    $neighborhood = trim($data->neighborhood ?? '');
    $bloodType = trim($data->bloodType ?? $data->blood_type ?? '');
    $phenotype = trim($data->phenotype ?? '');
    $weight = isset($data->weight) ? (float)$data->weight : null;
    $height = isset($data->height) ? (float)$data->height : null;
    $weightValue = ($weight !== null && $weight > 0) ? $weight : null;
    $heightValue = ($height !== null && $height > 0) ? $height : null;

    if (empty($dni) || empty($firstName) || empty($lastName) || empty($bloodType)) {
        sendJsonResponse("error", "Por favor complete los campos obligatorios (DNI, Nombre, Apellido, Grupo/Factor)", null, 400);
    }

    try {
        $conn->beginTransaction();

        // Check if donor exists
        $checkStmt = $conn->prepare("SELECT id FROM donors WHERE dni = :dni");
        $checkStmt->execute(['dni' => $dni]);
        $existing = $checkStmt->fetch();

        if ($existing) {
            $donorId = (int)$existing['id'];
            $updateStmt = $conn->prepare("UPDATE donors SET first_name = :firstName, last_name = :lastName, gender = :gender, birth_date = :birthDate, city = :city, zip_code = :zipCode, address = :address, neighborhood = :neighborhood, blood_type = :bloodType, phenotype = :phenotype, weight = :weight, height = :height WHERE id = :id");
            $updateStmt->execute([
                'firstName' => $firstName,
                'lastName' => $lastName,
                'gender' => $gender,
                'birthDate' => $birthDate,
                'city' => $city,
                'zipCode' => $zipCode,
                'address' => $address,
                'neighborhood' => $neighborhood,
                'bloodType' => $bloodType,
                'phenotype' => $phenotype,
                'weight' => $weightValue,
                'height' => $heightValue,
                'id' => $donorId
            ]);
        } else {
            $insertStmt = $conn->prepare("INSERT INTO donors (dni, first_name, last_name, gender, birth_date, city, zip_code, address, neighborhood, blood_type, phenotype, weight, height, status) VALUES (:dni, :firstName, :lastName, :gender, :birthDate, :city, :zipCode, :address, :neighborhood, :bloodType, :phenotype, :weight, :height, 'APTO')");
            $insertStmt->execute([
                'dni' => $dni,
                'firstName' => $firstName,
                'lastName' => $lastName,
                'gender' => $gender,
                'birthDate' => $birthDate,
                'city' => $city,
                'zipCode' => $zipCode,
                'address' => $address,
                'neighborhood' => $neighborhood,
                'bloodType' => $bloodType,
                'phenotype' => $phenotype,
                'weight' => $weightValue,
                'height' => $heightValue
            ]);
            $donorId = (int)$conn->lastInsertId();
        }

        // Save nested phones if provided
        if (isset($data->phones) && is_array($data->phones)) {
            $conn->prepare("DELETE FROM donor_phones WHERE donor_id = :donorId")->execute(['donorId' => $donorId]);
            $phoneStmt = $conn->prepare("INSERT INTO donor_phones (donor_id, phone_number, phone_type, is_primary) VALUES (:donorId, :phone, :type, :primary)");
            foreach ($data->phones as $phoneObj) {
                $num = trim($phoneObj->phoneNumber ?? $phoneObj->phone_number ?? '');
                $type = trim($phoneObj->phoneType ?? $phoneObj->phone_type ?? 'Mobile');
                $isPrimary = !empty($phoneObj->isPrimary ?? $phoneObj->is_primary) ? 1 : 0;
                if (!empty($num)) {
                    $phoneStmt->execute(['donorId' => $donorId, 'phone' => $num, 'type' => $type, 'primary' => $isPrimary]);
                }
            }
        }

        // Save nested emails if provided
        if (isset($data->emails) && is_array($data->emails)) {
            $conn->prepare("DELETE FROM donor_emails WHERE donor_id = :donorId")->execute(['donorId' => $donorId]);
            $emailStmt = $conn->prepare("INSERT INTO donor_emails (donor_id, email_address, email_type, is_primary) VALUES (:donorId, :email, :type, :primary)");
            foreach ($data->emails as $emailObj) {
                $addr = trim($emailObj->emailAddress ?? $emailObj->email_address ?? '');
                $type = trim($emailObj->emailType ?? $emailObj->email_type ?? 'Personal');
                $isPrimary = !empty($emailObj->isPrimary ?? $emailObj->is_primary) ? 1 : 0;
                if (!empty($addr)) {
                    $emailStmt->execute(['donorId' => $donorId, 'email' => $addr, 'type' => $type, 'primary' => $isPrimary]);
                }
            }
        }

        $conn->commit();

        sendJsonResponse("success", "Datos del donante guardados correctamente", ["donorId" => $donorId, "dni" => $dni]);
    } catch(PDOException $e) {
        if ($conn->inTransaction()) {
            $conn->rollBack();
        }
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
}
?>