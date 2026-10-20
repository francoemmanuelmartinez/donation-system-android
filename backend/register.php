<?php
require_once 'db_config.php';

$json = file_get_contents('php://input');
$data = json_decode($json);

if (!$data) {
    sendJsonResponse("error", "Invalid JSON payload", null, 400);
}

$firstName = trim($data->firstName ?? $data->first_name ?? '');
$lastName = trim($data->lastName ?? $data->last_name ?? '');
$dni = trim($data->dni ?? '');
$email = trim($data->email ?? '');
$phone = trim($data->phone ?? '');
$password = trim($data->password ?? '');

if (empty($firstName) || empty($lastName) || empty($dni) || empty($email) || empty($password)) {
    sendJsonResponse("error", "Por favor complete todos los campos obligatorios", null, 400);
}

// Check if DNI or Email already exists
$checkStmt = $conn->prepare("SELECT id FROM users WHERE dni = :dni OR email = :email");
$checkStmt->execute(['dni' => $dni, 'email' => $email]);
if ($checkStmt->fetch()) {
    sendJsonResponse("error", "El DNI o Correo Electrónico ya se encuentra registrado", null, 409);
}

$passwordHash = password_hash($password, PASSWORD_DEFAULT);

try {
    $conn->beginTransaction();

    $stmt = $conn->prepare("INSERT INTO users (first_name, last_name, dni, email, password_hash, role, is_approved) VALUES (:firstName, :lastName, :dni, :email, :passwordHash, 'ADMINISTRATIVA', 0)");
    $stmt->execute([
        'firstName' => $firstName,
        'lastName' => $lastName,
        'dni' => $dni,
        'email' => $email,
        'passwordHash' => $passwordHash
    ]);

    $userId = $conn->lastInsertId();

    if (!empty($phone)) {
        $phoneStmt = $conn->prepare("INSERT INTO user_phones (user_id, phone_number, phone_type, is_primary) VALUES (:userId, :phone, 'Mobile', 1)");
        $phoneStmt->execute(['userId' => $userId, 'phone' => $phone]);
    }

    $emailStmt = $conn->prepare("INSERT INTO user_emails (user_id, email_address, email_type, is_primary) VALUES (:userId, :email, 'Personal', 1)");
    $emailStmt->execute(['userId' => $userId, 'email' => $email]);

    $conn->commit();

    sendJsonResponse("success", "Cuenta registrada con éxito. Esperando aprobación de Dirección.", ["userId" => (int)$userId]);
} catch(PDOException $e) {
    if ($conn->inTransaction()) {
        $conn->rollBack();
    }
    sendJsonResponse("error", "Error en la base de datos: " . $e->getMessage(), null, 500);
}
?>