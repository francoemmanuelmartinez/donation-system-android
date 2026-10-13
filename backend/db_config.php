<?php
// db_config.php
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

$requestMethod = $_SERVER['REQUEST_METHOD'] ?? '';
if ($requestMethod === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Zona horaria del banco (Argentina). PHP en XAMPP viene con UTC/Europe-Berlin
// por defecto y MySQL corre en hora local: fijarse explícitamente evita
// desfases de 5h en la ventana de envío y en el cálculo de HOY.
date_default_timezone_set('America/Argentina/Buenos_Aires');

$host = "localhost";
$db_name = "donation_system";
$username = "root";
$password = "";

// Optional local override (e.g. point the API at a dedicated test database).
// Create backend/config_override.db.php to define $host, $db_name, $username, $password.
if (file_exists(__DIR__ . '/config_override.db.php')) {
    require_once __DIR__ . '/config_override.db.php';
}

try {
    // Timeout de conexion corto: si MySQL deja de responder, la API falla en
    // segundos con 500 en vez de colgar la request hasta el timeout del cliente.
    $options = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_TIMEOUT            => 5,
    ];
    $conn = new PDO("mysql:host=$host;dbname=$db_name;charset=utf8mb4", $username, $password, $options);
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database Connection Failed: " . $e->getMessage()
    ]);
    exit();
}

function sendJsonResponse($status, $message, $data = null, $statusCode = 200) {
    http_response_code($statusCode);
    $response = [
        "status" => $status,
        "message" => $message
    ];
    if ($data !== null) {
        $response["data"] = $data;
    }
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit();
}

function calculateEligibleWorkdayReturnDate($startDateStr, $vedaDays) {
    $date = new DateTime($startDateStr);
    $date->modify("+$vedaDays days");

    // N: 1 (Mon) through 7 (Sun)
    $dayOfWeek = (int)$date->format('N');
    if ($dayOfWeek === 6) { // Saturday -> advance 2 days to Monday
        $date->modify("+2 days");
    } else if ($dayOfWeek === 7) { // Sunday -> advance 1 day to Monday
        $date->modify("+1 day");
    }
    return $date->format('Y-m-d');
}
?>