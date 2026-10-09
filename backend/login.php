<?php
require_once 'db_config.php';

$json = file_get_contents('php://input');
$data = json_decode($json);

if (!$data) {
    sendJsonResponse("error", "Invalid JSON payload", null, 400);
}

$emailInput = trim($data->email ?? $data->username ?? '');
$passwordInput = trim($data->password ?? '');

if (empty($emailInput) || empty($passwordInput)) {
    sendJsonResponse("error", "Por favor ingrese correo electrónico y contraseña", null, 400);
}

try {
    $stmt = $conn->prepare("SELECT * FROM users WHERE email = :input OR dni = :input LIMIT 1");
    $stmt->execute(['input' => $emailInput]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($passwordInput, $user['password_hash'])) {
        sendJsonResponse("error", "Credenciales inválidas. Verifique su correo y contraseña.", null, 401);
    }

    if ((int)$user['is_approved'] !== 1) {
        sendJsonResponse("error", "Cuenta pendiente de aprobación por la Dirección.", null, 403);
    }

    // Fetch user phones and emails
    $phoneStmt = $conn->prepare("SELECT phone_number, phone_type, is_primary FROM user_phones WHERE user_id = :userId");
    $phoneStmt->execute(['userId' => $user['id']]);
    $phones = array_map(function ($p) {
        return [
            'phoneNumber' => $p['phone_number'],
            'phoneType' => $p['phone_type'],
            'isPrimary' => (bool)$p['is_primary']
        ];
    }, $phoneStmt->fetchAll());

    $emailStmt = $conn->prepare("SELECT email_address, email_type, is_primary FROM user_emails WHERE user_id = :userId");
    $emailStmt->execute(['userId' => $user['id']]);
    $emails = array_map(function ($e) {
        return [
            'emailAddress' => $e['email_address'],
            'emailType' => $e['email_type'],
            'isPrimary' => (bool)$e['is_primary']
        ];
    }, $emailStmt->fetchAll());

    $userData = [
        "id" => (int)$user['id'],
        "firstName" => $user['first_name'],
        "lastName" => $user['last_name'],
        "dni" => $user['dni'],
        "email" => $user['email'],
        "role" => $user['role'],
        "isApproved" => (bool)$user['is_approved'],
        "phones" => $phones,
        "emails" => $emails
    ];

    sendJsonResponse("success", "¡Bienvenido de nuevo!", $userData);
} catch(PDOException $e) {
    sendJsonResponse("error", "Error en la base de datos: " . $e->getMessage(), null, 500);
}
?>