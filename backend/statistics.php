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

if (!in_array($range, ['day', 'week', 'month', 'year'])) {
    sendJsonResponse("error", "Rango inválido", null, 400);
}

try {
    list($fromDate, $toDate, $groupBySql, $groupByLabel) = computeRange($range);

    $stmt = $conn->prepare("SELECT COUNT(*) AS total FROM donations WHERE created_at BETWEEN :from AND :to");
    $stmt->execute(['from' => $fromDate, 'to' => $toDate]);
    $donationsTotal = (int)($stmt->fetch()['total'] ?? 0);

    $stmt = $conn->prepare("SELECT p.procedure_type AS procedureType, COUNT(*) AS cnt
                            FROM donation_procedures p
                            INNER JOIN donations d ON d.id = p.donation_id
                            WHERE d.created_at BETWEEN :from AND :to
                            GROUP BY p.procedure_type");
    $stmt->execute(['from' => $fromDate, 'to' => $toDate]);
    $procedureRows = $stmt->fetchAll();
    $procedureCounts = ['PLASMA' => 0, 'PLATELETS' => 0, 'BLOOD' => 0];
    foreach ($procedureRows as $row) {
        $type = (string)($row['procedureType'] ?? '');
        if (isset($procedureCounts[$type])) {
            $procedureCounts[$type] = (int)$row['cnt'];
        }
    }

    $stmt = $conn->prepare("SELECT COUNT(*) AS total FROM donors WHERE created_at BETWEEN :from AND :to");
    $stmt->execute(['from' => $fromDate, 'to' => $toDate]);
    $donorsRegistered = (int)($stmt->fetch()['total'] ?? 0);

    $stmt = $conn->query("SELECT COUNT(DISTINCT donor_id) AS total FROM donor_restrictions WHERE is_permanent = 1 OR eligible_workday_return_date >= CURDATE()");
    $donorsInVeda = (int)($stmt->fetch()['total'] ?? 0);

    $stmt = $conn->prepare("SELECT DATE_FORMAT(d.created_at, :fmt) AS date, COUNT(*) AS cnt
                            FROM donations d
                            WHERE d.created_at BETWEEN :from AND :to
                            GROUP BY DATE_FORMAT(d.created_at, :fmt)
                            ORDER BY date ASC");
    $stmt->execute(['fmt' => $groupBySql, 'from' => $fromDate, 'to' => $toDate]);
    $seriesRows = $stmt->fetchAll();
    $series = [];
    foreach ($seriesRows as $row) {
        $series[] = ['date' => $row['date'], 'count' => (int)$row['cnt']];
    }

    sendJsonResponse("success", "Estadísticas recuperadas", [
        'range' => $range,
        'fromDate' => $fromDate,
        'toDate' => $toDate,
        'donationsTotal' => $donationsTotal,
        'procedureCounts' => $procedureCounts,
        'donorsRegistered' => $donorsRegistered,
        'donorsInVeda' => $donorsInVeda,
        'series' => $series
    ]);
} catch(PDOException $e) {
    sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
}
?>