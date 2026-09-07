<?php
header('Content-Type: application/json');
require_once 'db_config.php';

$json = file_get_contents('php://input');
$data = json_decode($json);

if (!$data || !isset($data->username) || !isset($data->password)) {
    echo json_encode(["status" => "error", "message" => "Invalid data"]);
    exit;
}

$username = trim($data->username);
$password = trim($data->password);

// 1. Check if user already exists
$checkStmt = $conn->prepare("SELECT id FROM users WHERE username = :username");
$checkStmt->execute(['username' => $username]);
if ($checkStmt->fetch()) {
    http_response_code(409); // Conflict
    echo json_encode(["status" => "error", "message" => "Username already exists"]);
    exit;
}

// 2. Hash and Save
$hashedPassword = password_hash($password, PASSWORD_DEFAULT);

try {
    $stmt = $conn->prepare("INSERT INTO users (username, password) VALUES (:username, :password)");
    $stmt->execute(['username' => $username, 'password' => $hashedPassword]);
    echo json_encode(["status" => "success", "message" => "Registration successful!"]);
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database error"]);
}
?>