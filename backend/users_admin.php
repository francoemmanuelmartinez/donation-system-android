<?php
require_once 'db_config.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    if ($action === 'pending') {
        try {
            $stmt = $conn->query("SELECT id, first_name AS firstName, last_name AS lastName, dni, email, role, is_approved AS isApproved, created_at AS createdAt FROM users WHERE is_approved = 0 ORDER BY created_at DESC");
            $users = $stmt->fetchAll();
            sendJsonResponse("success", "Pending staff retrieved", $users);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else if ($action === 'active') {
        try {
            $stmt = $conn->query("SELECT id, first_name AS firstName, last_name AS lastName, dni, email, role, is_approved AS isApproved, created_at AS createdAt FROM users WHERE is_approved = 1 ORDER BY last_name ASC, first_name ASC");
            $users = $stmt->fetchAll();
            sendJsonResponse("success", "Active staff retrieved", $users);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else {
        sendJsonResponse("error", "Invalid action", null, 400);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $json = file_get_contents('php://input');
    $data = json_decode($json);

    if (!$data) {
        sendJsonResponse("error", "Invalid JSON payload", null, 400);
    }

    if ($action === 'approve') {
        $userId = (int)($data->userId ?? $data->user_id ?? 0);
        $role = trim($data->role ?? 'ADMINISTRATIVA');
        $approvedByUserId = isset($data->approvedByUserId) ? (int)$data->approvedByUserId : null;

        if ($userId <= 0) {
            sendJsonResponse("error", "Invalid user ID", null, 400);
        }

        if (!in_array($role, ['ADMINISTRATIVA', 'DIRECTORA'])) {
            sendJsonResponse("error", "Rol no válido", null, 400);
        }

        try {
            $stmt = $conn->prepare("UPDATE users SET is_approved = 1, role = :role, approved_by_user_id = :approvedBy WHERE id = :id");
            $stmt->execute([
                'role' => $role,
                'approvedBy' => $approvedByUserId,
                'id' => $userId
            ]);

            sendJsonResponse("success", "Usuario aprobado exitosamente", ["userId" => $userId, "role" => $role]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else if ($action === 'delete') {
        $userId = (int)($data->userId ?? $data->user_id ?? 0);

        if ($userId <= 0) {
            sendJsonResponse("error", "Invalid user ID", null, 400);
        }

        try {
            // Check target user's role
            $checkStmt = $conn->prepare("SELECT role, is_approved FROM users WHERE id = :id");
            $checkStmt->execute(['id' => $userId]);
            $targetUser = $checkStmt->fetch();

            if (!$targetUser) {
                sendJsonResponse("error", "Usuario no encontrado", null, 404);
            }

            // CRITICAL SECURITY MUTEX ENFORCEMENT
            if ($targetUser['role'] === 'DIRECTORA' && (int)$targetUser['is_approved'] === 1) {
                $countStmt = $conn->query("SELECT COUNT(*) AS director_count FROM users WHERE role = 'DIRECTORA' AND is_approved = 1");
                $result = $countStmt->fetch();
                $directorCount = (int)($result['director_count'] ?? 0);

                if ($directorCount <= 1) {
                    sendJsonResponse("error", "Operación denegada. No se puede eliminar al único usuario con rol de Directora en el sistema.", null, 403);
                }
            }

            $deleteStmt = $conn->prepare("DELETE FROM users WHERE id = :id");
            $deleteStmt->execute(['id' => $userId]);

            sendJsonResponse("success", "Usuario eliminado exitosamente", ["userId" => $userId]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else {
        sendJsonResponse("error", "Invalid action", null, 400);
    }
}
?>