<?php
require_once 'db_config.php';

$action = $_GET['action'] ?? $_POST['action'] ?? '';

if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    if ($action === 'pending') {
        try {
            $stmt = $conn->query("SELECT id, first_name AS firstName, last_name AS lastName, dni, email, role, is_approved AS isApproved, center_id AS centerId, created_at AS createdAt FROM users WHERE is_approved = 0 ORDER BY created_at DESC");
            $users = array_map(function ($u) {
                $u['isApproved'] = (bool)$u['isApproved'];
                return $u;
            }, $stmt->fetchAll());
            sendJsonResponse("success", "Pending staff retrieved", $users);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else if ($action === 'active') {
        try {
            $stmt = $conn->query("SELECT id, first_name AS firstName, last_name AS lastName, dni, email, role, is_approved AS isApproved, center_id AS centerId, created_at AS createdAt FROM users WHERE is_approved = 1 ORDER BY last_name ASC, first_name ASC");
            $users = array_map(function ($u) {
                $u['isApproved'] = (bool)$u['isApproved'];
                return $u;
            }, $stmt->fetchAll());
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

    if (isset($data->action)) {
        $action = trim($data->action);
    }

    if ($action === 'approve') {
        $userId = (int)($data->userId ?? $data->user_id ?? 0);
        $role = trim($data->role ?? 'ADMINISTRATIVA');
        $approvedByUserId = isset($data->approvedByUserId) ? (int)$data->approvedByUserId : null;
        $centerId = (int)($data->centerId ?? $data->center_id ?? 0);
        if ($centerId <= 0 && $approvedByUserId !== null) {
            // Default to the approver's center
            $ctrStmt = $conn->prepare("SELECT center_id FROM users WHERE id = :id");
            $ctrStmt->execute(['id' => $approvedByUserId]);
            $ctrRow = $ctrStmt->fetch();
            if ($ctrRow && $ctrRow['center_id'] !== null) {
                $centerId = (int)$ctrRow['center_id'];
            }
        }
        if ($centerId <= 0) {
            $centerId = 1; // fallback to default center
        }

        if ($userId <= 0) {
            sendJsonResponse("error", "Invalid user ID", null, 400);
        }

        if (!in_array($role, ['ADMINISTRATIVA', 'DIRECTORA', 'SUPERADMIN'])) {
            sendJsonResponse("error", "Rol no válido", null, 400);
        }

        try {
            // Set center_id if provided, otherwise keep existing
            $stmt = $conn->prepare("UPDATE users SET is_approved = 1, role = :role, approved_by_user_id = :approvedBy, center_id = :centerId WHERE id = :id");
            $stmt->execute([
                'role' => $role,
                'approvedBy' => $approvedByUserId,
                'centerId' => $centerId,
                'id' => $userId
            ]);

            sendJsonResponse("success", "Usuario aprobado exitosamente", ["userId" => $userId, "role" => $role]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else if ($action === 'update_role') {
        $userId = (int)($data->userId ?? $data->user_id ?? 0);
        $role = trim($data->role ?? '');

        if ($userId <= 0) {
            sendJsonResponse("error", "Invalid user ID", null, 400);
        }

        if (!in_array($role, ['ADMINISTRATIVA', 'DIRECTORA', 'SUPERADMIN'])) {
            sendJsonResponse("error", "Rol no válido", null, 400);
        }

        try {
            $checkStmt = $conn->prepare("SELECT role, is_approved, center_id FROM users WHERE id = :id");
            $checkStmt->execute(['id' => $userId]);
            $targetUser = $checkStmt->fetch();

            if (!$targetUser) {
                sendJsonResponse("error", "Usuario no encontrado", null, 404);
            }

            // CRITICAL SECURITY MUTEX ENFORCEMENT
            if ($targetUser['role'] === 'DIRECTORA' && (int)$targetUser['is_approved'] === 1 && $role !== 'DIRECTORA') {
                $countStmt = $conn->prepare("SELECT COUNT(*) AS director_count FROM users WHERE role = 'DIRECTORA' AND is_approved = 1 AND center_id = :centerId");
                $countStmt->execute(['centerId' => $targetUser['center_id']]);
                $result = $countStmt->fetch();
                $directorCount = (int)($result['director_count'] ?? 0);

                if ($directorCount <= 1) {
                    sendJsonResponse("error", "Operación denegada. No se puede degradar al único usuario con rol de Directora en el sistema.", null, 403);
                }
            }

            if ($targetUser['role'] === 'SUPERADMIN' && (int)$targetUser['is_approved'] === 1 && $role !== 'SUPERADMIN') {
                $countStmt = $conn->query("SELECT COUNT(*) AS superadmin_count FROM users WHERE role = 'SUPERADMIN' AND is_approved = 1");
                $result = $countStmt->fetch();
                $superadminCount = (int)($result['superadmin_count'] ?? 0);

                if ($superadminCount <= 1) {
                    sendJsonResponse("error", "Operación denegada. No se puede degradar al único usuario con rol de Superadmin en el sistema.", null, 403);
                }
            }

            $centerId = (int)($data->centerId ?? $data->center_id ?? 0);
            if ($centerId > 0) {
                $updateStmt = $conn->prepare("UPDATE users SET role = :role, center_id = :centerId WHERE id = :id");
                $updateStmt->execute(['role' => $role, 'centerId' => $centerId, 'id' => $userId]);
            } else {
                $updateStmt = $conn->prepare("UPDATE users SET role = :role WHERE id = :id");
                $updateStmt->execute(['role' => $role, 'id' => $userId]);
            }

            sendJsonResponse("success", "Rol actualizado exitosamente", ["userId" => $userId, "role" => $role]);
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
                $countStmt = $conn->prepare("SELECT COUNT(*) AS director_count FROM users WHERE role = 'DIRECTORA' AND is_approved = 1 AND center_id = :centerId");
                $countStmt->execute(['centerId' => $targetUser['center_id']]);
                $result = $countStmt->fetch();
                $directorCount = (int)($result['director_count'] ?? 0);

                if ($directorCount <= 1) {
                    sendJsonResponse("error", "Operación denegada. No se puede eliminar al único usuario con rol de Directora en el sistema.", null, 403);
                }
            }

            if ($targetUser['role'] === 'SUPERADMIN' && (int)$targetUser['is_approved'] === 1) {
                $countStmt = $conn->query("SELECT COUNT(*) AS superadmin_count FROM users WHERE role = 'SUPERADMIN' AND is_approved = 1");
                $result = $countStmt->fetch();
                $superadminCount = (int)($result['superadmin_count'] ?? 0);

                if ($superadminCount <= 1) {
                    sendJsonResponse("error", "Operación denegada. No se puede eliminar al único usuario con rol de Superadmin en el sistema.", null, 403);
                }
            }

            $deleteStmt = $conn->prepare("DELETE FROM users WHERE id = :id");
            $deleteStmt->execute(['id' => $userId]);

            sendJsonResponse("success", "Usuario eliminado exitosamente", ["userId" => $userId]);
        } catch(PDOException $e) {
            sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
        }
    } else if ($action === 'centers') {
        // ABM de centros (solo superadmin)
        $requesterRole = trim($data->requesterRole ?? '');
        if ($requesterRole !== 'SUPERADMIN') {
            sendJsonResponse("error", "Operación denegada. Solo Superadmin puede gestionar centros.", null, 403);
        }

        $centerAction = trim($data->centerAction ?? 'list');

        if ($centerAction === 'list') {
            try {
                $stmt = $conn->query("SELECT id, name, address, is_active AS isActive, created_at AS createdAt FROM centers ORDER BY name ASC");
                $centers = array_map(function ($c) {
                    $c['isActive'] = (bool)$c['isActive'];
                    return $c;
                }, $stmt->fetchAll());
                sendJsonResponse("success", "Centros recuperados", $centers);
            } catch(PDOException $e) {
                sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
            }
        } else if ($centerAction === 'create') {
            $name = trim($data->name ?? '');
            $address = trim($data->address ?? '');
            if (empty($name)) {
                sendJsonResponse("error", "El nombre del centro es obligatorio", null, 400);
            }
            try {
                $stmt = $conn->prepare("INSERT INTO centers (name, address) VALUES (:name, :address)");
                $stmt->execute(['name' => $name, 'address' => $address]);
                sendJsonResponse("success", "Centro creado exitosamente", ["centerId" => (int)$conn->lastInsertId()]);
            } catch(PDOException $e) {
                sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
            }
        } else if ($centerAction === 'update') {
            $centerId = (int)($data->centerId ?? $data->center_id ?? 0);
            $name = trim($data->name ?? '');
            $address = trim($data->address ?? '');
            $isActive = isset($data->isActive) ? (int)$data->isActive : 1;
            if ($centerId <= 0 || empty($name)) {
                sendJsonResponse("error", "Datos inválidos", null, 400);
            }
            try {
                $stmt = $conn->prepare("UPDATE centers SET name = :name, address = :address, is_active = :isActive WHERE id = :id");
                $stmt->execute(['name' => $name, 'address' => $address, 'isActive' => $isActive, 'id' => $centerId]);
                sendJsonResponse("success", "Centro actualizado exitosamente", ["centerId" => $centerId]);
            } catch(PDOException $e) {
                sendJsonResponse("error", "Database error: " . $e->getMessage(), null, 500);
            }
        } else {
            sendJsonResponse("error", "Acción de centro no válida", null, 400);
        }
    } else {
        sendJsonResponse("error", "Invalid action", null, 400);
    }
}