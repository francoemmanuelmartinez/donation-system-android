<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $q        = trim($_GET['q'] ?? '');
    $dni      = trim($_GET['dni'] ?? '');
    $name     = trim($_GET['name'] ?? '');
    $lastName = trim($_GET['last_name'] ?? '');
    $group    = strtoupper(trim($_GET['group']  ?? ''));
    $factor   = strtoupper(trim($_GET['factor'] ?? ''));
$bloodType = strtoupper(trim($_GET['blood_type'] ?? $_GET['bloodType'] ?? ''));
$centerId  = (int)($_GET['centerId'] ?? $_GET['center_id'] ?? 0);

$limit  = (int)($_GET['limit'] ?? 20);
    $offset = (int)($_GET['offset'] ?? 0);
    if ($limit < 1)  { $limit = 20; }
    if ($limit > 100) { $limit = 100; }
    if ($offset < 0) { $offset = 0; }

    $hasText = ($q !== '' || $dni !== '' || $name !== '' || $lastName !== '');

    try {
        $where = [];
        $params = [];
        // Búsqueda libre: coincide por DNI, nombre o apellido (OR).
        if ($q !== '') {
            $where[] = '(d.dni LIKE :q1 OR d.first_name LIKE :q2 OR d.last_name LIKE :q3)';
            $params['q1'] = $q . '%';
            $params['q2'] = $q . '%';
            $params['q3'] = $q . '%';
        }
        if ($dni !== '')      { $where[] = 'd.dni LIKE :dni';           $params['dni'] = $dni . '%'; }
        if ($name !== '')     { $where[] = 'd.first_name LIKE :name';   $params['name'] = $name . '%'; }
        if ($lastName !== '') { $where[] = 'd.last_name LIKE :lastName'; $params['lastName'] = $lastName . '%'; }

        if ($bloodType !== '') {
            $where[] = 'd.blood_type = :bloodType';
            $params['bloodType'] = $bloodType;
        } else if ($group !== '' || $factor !== '') {
            $where[] = 'd.blood_type LIKE :bloodPattern';
            $params['bloodPattern'] = ($group !== '' ? $group : '%') . ($factor !== '' ? $factor : '%');
        }

        if ($centerId > 0) {
            $where[] = 'd.center_id = :centerId';
            $params['centerId'] = $centerId;
        }

        $whereSql = empty($where) ? '' : (' WHERE ' . implode(' AND ', $where));

        // Estado (APTO / veda) calculado en SQL: fuente única de verdad, permite
        // ordenar y paginar de forma coherente sin traer toda la tabla.
        $sql = "SELECT
                    t.id, t.dni, t.firstName, t.lastName, t.bloodType, t.phenotype,
                    t.lastDonationDate, t.nextEligibleDate, t.phoneNumber,
                    t.recoveryEnd AS recoveryVedaEndDate,
                    IF(t.restrictionIsPermanent = 1, NULL, t.restrictionEndDate) AS restrictionVedaEndDate,
                    t.restrictionReason AS restrictionVedaReason,
                    t.restrictionIsPermanent AS isPermanentRestriction,
                    CASE
                        WHEN t.recoveryEnd IS NULL AND t.restrictionEndDate IS NULL THEN 'APTO'
                        WHEN t.recoveryEnd IS NOT NULL AND t.restrictionEndDate IS NOT NULL
                            THEN IF(t.recoveryEnd >= t.restrictionEndDate, 'VEDA_RECUPERACION', 'VEDA_RESTRICCION')
                        WHEN t.recoveryEnd IS NOT NULL THEN 'VEDA_RECUPERACION'
                        ELSE 'VEDA_RESTRICCION'
                    END AS state,
                    CASE
                        WHEN t.recoveryEnd IS NULL AND t.restrictionEndDate IS NULL THEN NULL
                        WHEN t.restrictionIsPermanent = 1 AND (t.recoveryEnd IS NULL OR t.restrictionEndDate >= t.recoveryEnd) THEN NULL
                        WHEN t.recoveryEnd IS NOT NULL AND t.restrictionEndDate IS NOT NULL
                            THEN IF(t.recoveryEnd >= t.restrictionEndDate, t.recoveryEnd, t.restrictionEndDate)
                        WHEN t.recoveryEnd IS NOT NULL THEN t.recoveryEnd
                        ELSE t.restrictionEndDate
                    END AS vedaEndDate,
                    CASE
                        WHEN t.recoveryEnd IS NULL AND t.restrictionEndDate IS NULL THEN NULL
                        WHEN t.recoveryEnd IS NOT NULL AND t.restrictionEndDate IS NOT NULL
                            THEN IF(t.recoveryEnd >= t.restrictionEndDate, 'Recuperación post-donación', t.restrictionReason)
                        WHEN t.recoveryEnd IS NOT NULL THEN 'Recuperación post-donación'
                        ELSE t.restrictionReason
                    END AS vedaReason
                FROM (
                    SELECT
                        d.id, d.dni, d.first_name AS firstName, d.last_name AS lastName,
                        d.blood_type AS bloodType, d.phenotype,
                        d.last_donation_date AS lastDonationDate,
                        d.next_eligible_date AS nextEligibleDate,
                        dp.phone_number AS phoneNumber,
                        IF(d.next_eligible_date IS NOT NULL AND d.next_eligible_date > CURDATE(), d.next_eligible_date, NULL) AS recoveryEnd,
                        rr.restrictionEndDate, rr.restrictionReason, rr.restrictionIsPermanent
                    FROM donors d
                    LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1
                    LEFT JOIN (
                        SELECT dr.donor_id,
                               MAX(CASE WHEN dr.is_permanent = 1 THEN DATE '9999-12-31' ELSE dr.eligible_workday_return_date END) AS restrictionEndDate,
                               MAX(CASE WHEN dr.is_permanent = 1 THEN 1 ELSE 0 END) AS restrictionIsPermanent,
                               SUBSTRING_INDEX(GROUP_CONCAT(r.name ORDER BY dr.is_permanent DESC, dr.eligible_workday_return_date DESC, dr.id DESC), ',', 1) AS restrictionReason
                        FROM donor_restrictions dr
                        JOIN restrictions r ON dr.restriction_id = r.id
                        WHERE dr.is_permanent = 1 OR dr.eligible_workday_return_date >= CURDATE()
                        GROUP BY dr.donor_id
                    ) rr ON rr.donor_id = d.id" . $whereSql . "
                ) t";

        if ($hasText) {
            $sql .= " ORDER BY t.lastName ASC, t.firstName ASC, t.id ASC";
        } else {
            // Aptos primero (ultima donacion mas vieja primero, nunca donantes primero),
            // luego veda por fecha de fin ascendente.
            $sql .= " ORDER BY (state = 'APTO') DESC, t.lastDonationDate ASC, vedaEndDate ASC, t.id ASC";
        }

        // Pedimos uno de mas para saber si hay pagina siguiente sin contar toda la tabla.
        $fetchLimit = $limit + 1;
        $sql .= " LIMIT $fetchLimit OFFSET $offset";

        $stmt = $conn->prepare($sql);
        $stmt->execute($params);
        $rows = $stmt->fetchAll();

        $hasMore = count($rows) > $limit;
        if ($hasMore) {
            array_pop($rows);
        }

        // Proximo turno agendado (mas cercano) solo para los donantes de esta pagina.
        $nextApptByDonor = [];
        if (!empty($rows)) {
            $ids = array_map(function ($r) { return (int)$r['id']; }, $rows);
            $placeholders = implode(',', array_fill(0, count($ids), '?'));
            $apptStmt = $conn->prepare("SELECT donor_id, appointment_date, target_procedure FROM appointments WHERE status = 'SCHEDULED' AND appointment_date >= ? AND donor_id IN ($placeholders) ORDER BY appointment_date ASC, appointment_time ASC");
            $apptStmt->execute(array_merge([date('Y-m-d')], $ids));
            while ($a = $apptStmt->fetch()) {
                $did = (int)$a['donor_id'];
                if (!isset($nextApptByDonor[$did])) {
                    $nextApptByDonor[$did] = $a;
                }
            }
        }

        foreach ($rows as &$donor) {
            $donor['status'] = ($donor['state'] === 'APTO') ? 'APTO' : 'EN_VEDA';
            $appt = $nextApptByDonor[(int)$donor['id']] ?? null;
            $donor['nextAppointmentDate'] = $appt ? $appt['appointment_date'] : null;
            $donor['nextAppointmentProcedure'] = $appt ? $appt['target_procedure'] : null;
        }
        unset($donor);

        sendJsonResponse("success", "Donantes encontrados", [
            "items" => $rows,
            "hasMore" => $hasMore
        ]);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else {
    sendJsonResponse("error", "Invalid request method", null, 405);
}
?>
