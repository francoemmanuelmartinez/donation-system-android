<?php
header('Content-Type: application/json');
require_once 'db_config.php'; // Load the database connection

$json = file_get_contents('php://input');
$data = json_decode($json);

if (!$data || !isset($data->username) || !isset($data->password)) {
    echo json_encode(["status" => "error", "message" => "Invalid request format"]);
    exit;
}

$user_input = $data->username;
$pass_input = $data->password;

try {
    // 1. Prepare the SQL query (Safe from SQL Injection)
    $stmt = $conn->prepare("SELECT * FROM users WHERE username = :username LIMIT 1");
    $stmt->bindParam(':username', $user_input);
    $stmt->execute();
    
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    // 2. Check if user exists and password matches
    // DEBUG: This will show in your Android Logcat
    error_log("Input: " . $pass_input);
    error_log("Hash from DB: " . $user['password']);
    if ($user && password_verify($pass_input, $user['password'])) {
    // Success!
        echo json_encode([
            "status" => "success",
            "message" => "Login successful",
            "user" => [
                "id" => $user['id'],
                "username" => $user['username']
            ],
            "token" => "sample_token_" . bin2hex(random_bytes(16))
        ]);
    } else {
        http_response_code(401);
        echo json_encode(["status" => "error", "message" => "Invalid username or password"]);
    }
} catch(PDOException $e) {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Database error: " . $e->getMessage()]);
}
?>