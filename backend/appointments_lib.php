<?php
/**
 * Shared appointment lifecycle helpers (Agenda robusta, P1).
 * Used by restrictions.php and appointments.php. See plan-agenda-robusta.md (T2).
 */

/** Valida fecha/hora/procedimiento del turno. Devuelve mensaje de error o null. */
function validateAppointmentPayload($appointmentDate, $appointmentTime, $targetProcedure) {
    $dateTime = DateTime::createFromFormat('Y-m-d', $appointmentDate);
    if (!$dateTime || $dateTime->format('Y-m-d') !== $appointmentDate) {
        return "Fecha de turno inválida";
    }
    if ($dateTime->format('Y-m-d') < date('Y-m-d')) {
        return "No se puede agendar en una fecha pasada";
    }
    if (!preg_match('/^\d{1,2}:\d{2}(:\d{2})?$/', $appointmentTime)) {
        return "Hora de turno inválida";
    }
    if (!in_array($targetProcedure, ['BLOOD', 'PLASMA', 'PLATELETS'])) {
        return "Tipo de donación inválido";
    }
    return null;
}

/**
 * Crea el turno nuevo. Devuelve ['error'=>..,'status'=>..] o ['appointmentId'=>..].
 * $excludeAppointmentId: turno propio a excluir del chequeo de 2º turno/día (para update/reschedule).
 */
function createAppointmentRow($conn, $donorId, $appointmentDate, $appointmentTime, $targetProcedure, $centerId = null, $excludeAppointmentId = null) {
    $sql = "SELECT id FROM appointments WHERE donor_id = :donorId AND appointment_date = :date AND status <> 'CANCELLED'";
    $params = ['donorId' => $donorId, 'date' => $appointmentDate];
    if ($excludeAppointmentId !== null && (int)$excludeAppointmentId > 0) {
        $sql .= " AND id <> :excludeId";
        $params['excludeId'] = (int)$excludeAppointmentId;
    }
    $stmt = $conn->prepare($sql);
    $stmt->execute($params);
    if ($stmt->fetch()) {
        return ['error' => "El donante ya tiene un turno agendado para ese día", 'status' => 409];
    }

    $conn->prepare("INSERT INTO appointments (donor_id, appointment_date, appointment_time, target_procedure, status, center_id) VALUES (:donorId, :date, :time, :procedure, 'SCHEDULED', :centerId)")
         ->execute([
             'donorId'   => $donorId,
             'date'      => $appointmentDate,
             'time'      => $appointmentTime,
             'procedure' => $targetProcedure,
             'centerId'  => $centerId !== null ? (int)$centerId : null,
         ]);

    return ['appointmentId' => (int)$conn->lastInsertId()];
}

/**
 * Valida y cancela el turno. Solo cancela turnos SCHEDULED (D8); idempotente si ya estaba CANCELLED.
 * Registra auditoría (cancelled_at, cancelled_by_user_id, cancel_reason) y el vínculo de reagendado.
 * Devuelve mensaje de error o null.
 */
function cancelAppointment($conn, $appointmentId, $donorId, $reason = null, $userId = null, $rescheduleToId = null) {
    $stmt = $conn->prepare("SELECT status FROM appointments WHERE id = :id AND donor_id = :donorId");
    $stmt->execute(['id' => $appointmentId, 'donorId' => $donorId]);
    $row = $stmt->fetch();
    if (!$row) {
        return "El turno a cancelar no pertenece a este donante";
    }
    if ($row['status'] === 'CANCELLED') {
        return null;
    }
    if ($row['status'] !== 'SCHEDULED') {
        return "Solo se puede cancelar un turno agendado";
    }

    $conn->prepare("UPDATE appointments SET status = 'CANCELLED', cancelled_at = NOW(), cancelled_by_user_id = :userId, cancel_reason = :reason, rescheduled_to_appointment_id = :rescheduleTo WHERE id = :id")
         ->execute([
             'userId'       => $userId !== null ? (int)$userId : null,
             'reason'       => $reason,
             'rescheduleTo' => $rescheduleToId !== null ? (int)$rescheduleToId : null,
             'id'           => $appointmentId,
         ]);
    return null;
}

/**
 * Limpia la marca de solicitud de reagendado del donante (D18) sobre un turno.
 * Se usa cuando el operador ya reagendó o editó manualmente el turno.
 * Devuelve mensaje de error o null.
 */
function clearRescheduleRequest($conn, $appointmentId, $donorId = null) {
    $sql = "UPDATE appointments SET reschedule_requested_at = NULL WHERE id = :id";
    $params = ['id' => (int)$appointmentId];
    if ($donorId !== null && (int)$donorId > 0) {
        $sql .= " AND donor_id = :donorId";
        $params['donorId'] = (int)$donorId;
    }
    $conn->prepare($sql)->execute($params);
    return null;
}

/** Marca asistencia (ATTENDED/NO_SHOW) de un turno SCHEDULED. Devuelve mensaje de error o null. */
function markAppointmentAttendance($conn, $appointmentId, $donorId, $status, $userId = null) {
    if (!in_array($status, ['ATTENDED', 'NO_SHOW'])) {
        return "Estado de asistencia inválido";
    }
    $stmt = $conn->prepare("SELECT status FROM appointments WHERE id = :id AND donor_id = :donorId");
    $stmt->execute(['id' => $appointmentId, 'donorId' => $donorId]);
    $row = $stmt->fetch();
    if (!$row) {
        return "El turno no pertenece a este donante";
    }
    if ($row['status'] !== 'SCHEDULED') {
        return "Solo se puede marcar la asistencia de un turno agendado";
    }

    $conn->prepare("UPDATE appointments SET status = :status, updated_at = NOW() WHERE id = :id")
         ->execute(['status' => $status, 'id' => $appointmentId]);
    return null;
}

/**
 * Envía (best-effort) un aviso de texto libre al donante por WhatsApp cuando se
 * cancela o reagenda su turno (D17). NUNCA revierte la operación: se llama
 * después del COMMIT y cualquier fallo solo se registra en el log.
 *
 * @param string $context 'CANCELLED' | 'RESCHEDULED'
 * @param array  $details Fecha/hora/procedimiento nuevos (para RESCHEDULED)
 * @return array ['ok'=>bool, 'message'=>string]
 */
function notifyDonorAppointmentChange($conn, $donorId, $context, $details = []) {
    $disabled = (defined('REMINDER_ENABLED') && REMINDER_ENABLED === false);
    if (!file_exists(__DIR__ . '/whatsapp_sender.php')) {
        return ['ok' => false, 'message' => 'whatsapp_sender.php no disponible'];
    }
    require_once __DIR__ . '/whatsapp_sender.php';

    $stmt = $conn->prepare(
        "SELECT CONCAT(d.first_name, ' ', d.last_name) AS name, dp.phone_number AS phone
         FROM donors d
         LEFT JOIN donor_phones dp ON dp.donor_id = d.id AND dp.is_primary = 1
         WHERE d.id = :id"
    );
    $stmt->execute(['id' => $donorId]);
    $donor = $stmt->fetch();
    if (!$donor) {
        return ['ok' => false, 'message' => 'Donante no encontrado'];
    }

    $phone = normalizeWaNumber($donor['phone'] ?? '');
    if ($phone === '') {
        logAppointmentNotice('Sin teléfono para donante ' . $donorId . ' (' . $context . ')');
        return ['ok' => false, 'message' => 'El donante no tiene teléfono'];
    }
    if ($disabled) {
        logAppointmentNotice('Kill-switch activo (REMINDER_ENABLED=false); no se avisó al donante ' . $donorId . ' (' . $context . ')');
        return ['ok' => false, 'message' => 'Avisos desactivados'];
    }

    $name = trim((string)($donor['name'] ?? ''));
    $greeting = $name !== '' ? ('Hola ' . $name . '!') : 'Hola!';

    if ($context === 'RESCHEDULED') {
        $date = formatAppointmentDateForNotice($details['date'] ?? '');
        $time = (string)($details['time'] ?? '');
        $procedure = appointmentProcedureLabel($details['procedure'] ?? '');
        $text = $greeting . ' Tu turno fue reprogramado para el ' . $date . ' a las ' . $time
              . ' (' . $procedure . '). Cualquier duda, escribinos.';
    } else {
        $text = $greeting . ' Tu turno en el banco de sangre fue cancelado. Si querés, podés agendar uno nuevo cuando te sea posible.';
    }

    $result = sendWhatsappText($phone, $text);
    logAppointmentNotice(
        ($result['ok'] ? 'Aviso enviado' : 'Aviso falló') . ' al donante ' . $donorId
        . ' (' . $context . '): ' . ($result['message'] ?? '')
    );
    return $result;
}

/** Formatea una fecha Y-m-d a d/m/Y para el aviso; devuelve el original si no parsea. */
function formatAppointmentDateForNotice($date) {
    $date = trim((string)$date);
    if ($date === '') {
        return '';
    }
    $dt = DateTime::createFromFormat('Y-m-d', $date);
    return $dt ? $dt->format('d/m/Y') : $date;
}

/** Etiqueta legible del procedimiento para el aviso. */
function appointmentProcedureLabel($procedure) {
    switch (strtoupper(trim((string)$procedure))) {
        case 'PLASMA':    return 'plasma';
        case 'PLATELETS': return 'plaquetas';
        case 'BLOOD':     return 'sangre';
        default:          return (string)$procedure;
    }
}

/** Log best-effort de los avisos de cancelación/reagendado (nunca lanza). */
function logAppointmentNotice($message) {
    try {
        $path = defined('REMINDER_LOG_PATH') ? REMINDER_LOG_PATH : (__DIR__ . '/logs/reminders.log');
        $dir = dirname($path);
        if (!is_dir($dir)) {
            @mkdir($dir, 0777, true);
        }
        @file_put_contents($path, '[' . date('Y-m-d H:i:s') . '] [APPT-NOTICE] ' . $message . PHP_EOL, FILE_APPEND);
    } catch (Exception $e) {
        // silencio: el aviso es accesorio
    }
}

/**
 * Cancela (si corresponde) y/o crea el turno nuevo a partir del bloque
 * "reschedule" del request. Devuelve ['error'=>..,'status'=>..] o ['appointment'=>..].
 */
function handleReschedule($conn, $donorId, $reschedule, $centerId = null, $userId = null) {
    $result = [];

    $cancelId = (int)($reschedule->cancelAppointmentId ?? $reschedule->cancel_appointment_id ?? 0);
    if ($cancelId > 0) {
        $error = cancelAppointment($conn, $cancelId, $donorId, 'RESCHEDULED', $userId);
        if ($error !== null) {
            return ['error' => $error, 'status' => 400];
        }
        $result['cancelledAppointmentId'] = $cancelId;
    }

    $date     = trim($reschedule->appointmentDate ?? $reschedule->appointment_date ?? '');
    $time     = trim($reschedule->appointmentTime ?? $reschedule->appointment_time ?? '');
    $procedure = strtoupper(trim($reschedule->targetProcedure ?? $reschedule->target_procedure ?? ''));

    if ($date !== '' || $time !== '' || $procedure !== '') {
        $error = validateAppointmentPayload($date, $time, $procedure);
        if ($error !== null) {
            return ['error' => $error, 'status' => 400];
        }
        $created = createAppointmentRow($conn, $donorId, $date, $time, $procedure, $centerId, $cancelId > 0 ? $cancelId : null);
        if (isset($created['error'])) {
            return $created;
        }
        $result['appointmentId']   = $created['appointmentId'];
        $result['appointmentDate'] = $date;
        $result['appointmentTime'] = $time;
        $result['targetProcedure'] = $procedure;

        if ($cancelId > 0) {
            $conn->prepare("UPDATE appointments SET rescheduled_to_appointment_id = :newId WHERE id = :oldId")
                 ->execute(['newId' => $created['appointmentId'], 'oldId' => $cancelId]);
        }
    }

    return ['appointment' => $result];
}
