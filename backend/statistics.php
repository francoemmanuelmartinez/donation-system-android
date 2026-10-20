<?php
require_once 'db_config.php';

function computeRange($range) {
    $tz = new DateTimeZone('America/Argentina/Buenos_Aires');
    $now = new DateTime('now', $tz);

    switch ($range) {
        case 'day':
            $from = (new DateTime('today', $tz));
            $groupBySql = '%Y-%m-%d';
            $groupByLabel = 'Y-m-d';
            break;
        case 'week':
            $from = (new DateTime('today', $tz));
            $from->modify('monday this week');
            $groupBySql = '%Y-%m-%d';
            $groupByLabel = 'Y-m-d';
            break;
        case 'month':
            $from = (new DateTime('first day of this month', $tz));
            $groupBySql = '%Y-%m-%d';
            $groupByLabel = 'Y-m-d';
            break;
        case 'year':
        default:
            $from = (new DateTime('first day of January ' . $now->format('Y'), $tz));
            $groupBySql = '%Y-%m';
            $groupByLabel = 'Y-m';
            break;
    }

    return [
        $from->format('Y-m-d H:i:s'),
        $now->format('Y-m-d H:i:s'),
        $groupBySql,
        $groupByLabel
    ];
}

$range = trim($_GET['range'] ?? 'week');
$centerId = (int)($_GET['centerId'] ?? $_GET['center_id'] ?? 0);
$staffUserId = (int)($_GET['staffUserId'] ?? $_GET['staff_user_id'] ?? 0);

if (!in_array($range, ['day', 'week', 'month', 'year'])) {
    sendJsonResponse("error", "Rango inválido", null, 400);
}

try {
    list($fromDate, $toDate, $groupBySql, $groupByLabel) = computeRange($range);

    // Build filter conditions
    $conditions = ["created_at BETWEEN :from AND :to"];
    $params = ['from' => $fromDate, 'to' => $toDate];

    if ($centerId > 0) {
        $conditions[] = "center_id = :centerId";
        $params['centerId'] = $centerId;
    }
    if ($staffUserId > 0) {
        $conditions[] = "staff_user_id = :staffUserId";
        $params['staffUserId'] = $staffUserId;
    }
    $whereClause = implode(" AND ", $conditions);

    $stmt = $conn->prepare("SELECT COUNT(*) AS total FROM donations WHERE $whereClause");
    $stmt->execute($params);
    $donationsTotal = (int)($stmt->fetch()['total'] ?? 0);

    $stmt = $conn->prepare("SELECT p.procedure_type AS procedureType, COUNT(*) AS cnt
                            FROM donation_procedures p
                            INNER JOIN donations d ON d.id = p.donation_id
                            WHERE d.created_at BETWEEN :from AND :to" .
                            ($centerId > 0 ? " AND d.center_id = :centerId" : "") .
                            ($staffUserId > 0 ? " AND d.staff_user_id = :staffUserId" : "") .
                            " GROUP BY p.procedure_type");
    $stmt->execute($params);
    $procedureRows = $stmt->fetchAll();
    $procedureCounts = ['PLASMA' => 0, 'PLATELETS' => 0, 'BLOOD' => 0];
    foreach ($procedureRows as $row) {
        $type = (string)($row['procedureType'] ?? '');
        if (isset($procedureCounts[$type])) {
            $procedureCounts[$type] = (int)$row['cnt'];
        }
    }

    $stmt = $conn->prepare("SELECT COUNT(*) AS total FROM donors WHERE created_at BETWEEN :from AND :to" .
                            ($centerId > 0 ? " AND center_id = :centerId" : ""));
    $stmt->execute($params);
    $donorsRegistered = (int)($stmt->fetch()['total'] ?? 0);

    $donorsInVedaSql = "SELECT COUNT(DISTINCT donor_id) AS total FROM donor_restrictions WHERE (is_permanent = 1 OR eligible_workday_return_date >= CURDATE())";
    if ($centerId > 0) {
        $donorsInVedaSql .= " AND center_id = :centerId";
    }
    $stmt = $conn->prepare($donorsInVedaSql);
    $stmt->execute($centerId > 0 ? ['centerId' => $centerId] : []);
    $donorsInVeda = (int)($stmt->fetch()['total'] ?? 0);

    $stmt = $conn->prepare("SELECT DATE_FORMAT(d.created_at, :fmt) AS date, COUNT(*) AS cnt
                            FROM donations d
                            WHERE $whereClause
                            GROUP BY DATE_FORMAT(d.created_at, :fmt)
                            ORDER BY date ASC");
    $stmt->execute(array_merge($params, ['fmt' => $groupBySql]));
    $seriesRows = $stmt->fetchAll();
    $series = [];
    foreach ($seriesRows as $row) {
        $series[] = ['date' => $row['date'], 'count' => (int)$row['cnt']];
    }

    // byStaff: count donations per staff user
    $byStaffSql = "SELECT d.staff_user_id AS staffUserId, CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) AS staffName, COUNT(*) AS count
                   FROM donations d
                   LEFT JOIN users u ON u.id = d.staff_user_id
                   WHERE $whereClause
                   GROUP BY d.staff_user_id
                   ORDER BY count DESC";
    $stmt = $conn->prepare($byStaffSql);
    $stmt->execute($params);
    $byStaffRows = $stmt->fetchAll();
    $byStaff = [];
    foreach ($byStaffRows as $row) {
        $byStaff[] = [
            'staffUserId' => $row['staffUserId'] !== null ? (int)$row['staffUserId'] : null,
            'staffName' => trim($row['staffName']),
            'count' => (int)$row['count']
        ];
    }

    sendJsonResponse("success", "Estadísticas recuperadas", [
        'range' => $range,
        'fromDate' => $fromDate,
        'toDate' => $toDate,
        'donationsTotal' => $donationsTotal,
        'procedureCounts' => $procedureCounts,
        'donorsRegistered' => $donorsRegistered,
        'donorsInVeda' => $donorsInVeda,
        'series' => $series,
        'byStaff' => $byStaff
    ]);
} catch(PDOException $e) {
    sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
}
?>