<?php
require_once 'db_config.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    $userId = (int)($_GET['userId'] ?? $_GET['user_id'] ?? 0);

    if ($userId <= 0) {
        sendJsonResponse("error", "Invalid user ID", null, 400);
    }

    try {
        $stmt = $conn->prepare("SELECT u.id, u.first_name AS firstName, u.last_name AS lastName, u.dni, u.email, u.role, u.is_approved AS isApproved, u.center_id AS centerId, c.name AS centerName FROM users u LEFT JOIN centers c ON c.id = u.center_id WHERE u.id = :id LIMIT 1");
        $stmt->execute(['id' => $userId]);
        $user = $stmt->fetch();

        if (!$user) {
            sendJsonResponse("error", "Usuario no encontrado", null, 404);
        }

        // Phones
        $phoneStmt = $conn->prepare("SELECT phone_number, phone_type, is_primary FROM user_phones WHERE user_id = :userId");
        $phoneStmt->execute(['userId' => $userId]);
        $phones = array_map(function ($p) {
            return [
                'phoneNumber' => $p['phone_number'],
                'phoneType' => $p['phone_type'],
                'isPrimary' => (bool)$p['is_primary']
            ];
        }, $phoneStmt->fetchAll());

        // Emails
        $emailStmt = $conn->prepare("SELECT email_address, email_type, is_primary FROM user_emails WHERE user_id = :userId");
        $emailStmt->execute(['userId' => $userId]);
        $emails = array_map(function ($e) {
            return [
                'emailAddress' => $e['email_address'],
                'emailType' => $e['email_type'],
                'isPrimary' => (bool)$e['is_primary']
            ];
        }, $emailStmt->fetchAll());

        $user['isApproved'] = (bool)$user['isApproved'];
        $user['centerId'] = $user['centerId'] !== null ? (int)$user['centerId'] : null;
        $user['phones'] = $phones;
        $user['emails'] = $emails;

        sendJsonResponse("success", "Perfil recuperado", $user);
    } catch(PDOException $e) {
        sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'POST' || $_SERVER['REQUEST_METHOD'] === 'PUT') {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data) {
        sendJsonResponse("error", "Invalid JSON payload", null, 400);
    }

    $userId = (int)($data->userId ?? $data->user_id ?? 0);
    $firstName = trim($data->firstName ?? $data->first_name ?? '');
    $lastName = trim($data->lastName ?? $data->last_name ?? '');
    $email = trim($data->email ?? '');
    $phone = trim($data->phone ?? '');

    if ($userId <= 0) {
        sendJsonResponse("error", "Invalid user ID", null, 400);
    }

    if (empty($firstName) || empty($lastName) || empty($email)) {
        sendJsonResponse("error", "Nombre, apellido y email son obligatorios", null, 400);
    }

    try {
        $conn->beginTransaction();

        // Check if email is already used by another user
        $checkStmt = $conn->prepare("SELECT id FROM users WHERE email = :email AND id <> :id");
        $checkStmt->execute(['email' => $email, 'id' => $userId]);
        if ($checkStmt->fetch()) {
            sendJsonResponse("error", "El correo electrónico ya está en uso por otro usuario", null, 409);
        }

        // Update user
        $stmt = $conn->prepare("UPDATE users SET first_name = :firstName, last_name = :lastName, email = :email WHERE id = :id");
        $stmt->execute([
            'firstName' => $firstName,
            'lastName' => $lastName,
            'email' => $email,
            'id' => $userId
        ]);

        // Update primary phone (if provided)
        if (!empty($phone)) {
            $phoneCheck = $conn->prepare("SELECT id FROM user_phones WHERE user_id = :userId AND is_primary = 1 LIMIT 1");
            $phoneCheck->execute(['userId' => $userId]);
            $existingPhone = $phoneCheck->fetch();

            if ($existingPhone) {
                $phoneStmt = $conn->prepare("UPDATE user_phones SET phone_number = :phone WHERE user_id = :userId AND is_primary = 1");
                $phoneStmt->execute(['phone' => $phone, 'userId' => $userId]);
            } else {
                $phoneStmt = $conn->prepare("INSERT INTO user_phones (user_id, phone_number, phone_type, is_primary) VALUES (:userId, :phone, 'Mobile', 1)");
                $phoneStmt->execute(['userId' => $userId, 'phone' => $phone]);
            }
        }

        // Update primary email in user_emails (if email changed)
        $emailCheck = $conn->prepare("SELECT id FROM user_emails WHERE user_id = :userId AND is_primary = 1 LIMIT 1");
        $emailCheck->execute(['userId' => $userId]);
        $existingEmail = $emailCheck->fetch();

        if ($existingEmail) {
            $emailStmt = $conn->prepare("UPDATE user_emails SET email_address = :email WHERE user_id = :userId AND is_primary = 1");
            $emailStmt->execute(['email' => $email, 'userId' => $userId]);
        } else {
            $emailStmt = $conn->prepare("INSERT INTO user_emails (user_id, email_address, email_type, is_primary) VALUES (:userId, :email, 'Personal', 1)");
            $emailStmt->execute(['userId' => $userId, 'email' => $email]);
        }

        // Also update the main users.email field (already done above)
        // Update users.email to match
        $conn->prepare("UPDATE users SET email = :email WHERE id = :id")->execute(['email' => $email, 'id' => $userId]);

        $conn->commit();
        sendJsonResponse("success", "Perfil actualizado exitosamente", ["userId" => $userId]);
    } catch(PDOException $e) {
        if ($conn->inTransaction()) {
            $conn->rollBack();
        }
        sendJsonResponse("error", "Error en la base de datos: " . $e->getMessage(), null, 500);
    }
} else {
    sendJsonResponse("error", "Method not allowed", null, 405);
}
?>
