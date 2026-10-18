<?php
// openwa-demo/backend/openwa_client.php
//
// El que realmente manda el mensaje a OpenWA, usando cURL.
require_once __DIR__ . '/openwa_config.php';

/**
 * Deja el número "prolijo": solo dígitos y con el código de país.
 * Ej: "(54) 911 5555-5555" -> "5491155555555".
 * Si tiene entre 10 y 12 dígitos y no arranca con "54", se lo agrega (Argentina).
 */
function normalizarNumero($telefono) {
    $soloDigitos = preg_replace('/[^0-9]/', '', (string)$telefono);
    if ($soloDigitos === '') {
        return '';
    }
    if (strlen($soloDigitos) >= 10 && strlen($soloDigitos) <= 12 && strpos($soloDigitos, '54') !== 0) {
        return '54' . $soloDigitos;
    }
    return $soloDigitos;
}

/**
 * Envía un texto por WhatsApp usando OpenWA.
 *
 * @return array ['ok' => true/false, 'mensaje' => 'explicación']
 */
function enviarWhatsApp($numero, $texto) {
    // 1) ¿Está disponible cURL?
    if (!function_exists('curl_init')) {
        return ['ok' => false, 'mensaje' => 'La extensión cURL no está activada en PHP'];
    }

    // 2) ¿Configuraste bien la sesión?
    if (OPENWA_SESSION_ID === '' || OPENWA_SESSION_ID === 'TU_SESSION_ID') {
        return ['ok' => false, 'mensaje' => 'Falta configurar OPENWA_SESSION_ID'];
    }

    // 3) Preparar el número y el texto
    $numero = normalizarNumero($numero);
    if ($numero === '') {
        return ['ok' => false, 'mensaje' => 'El teléfono está vacío o es inválido'];
    }

    // 4) Armar la dirección ("endpoint") de OpenWA y el cuerpo del pedido
    $url = rtrim(OPENWA_BASE_URL, '/') . '/api/sessions/' . OPENWA_SESSION_ID . '/messages/send-text';
    $cuerpo = json_encode([
        'chatId' => $numero . '@c.us',
        'text'   => $texto
    ], JSON_UNESCAPED_UNICODE);

    // 5) Hacer el pedido con cURL
    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => $cuerpo,
        CURLOPT_HTTPHEADER     => [
            'Content-Type: application/json',
            'X-API-Key: ' . OPENWA_API_KEY
        ],
        CURLOPT_TIMEOUT        => 20,
        CURLOPT_CONNECTTIMEOUT => 5
    ]);
    $respuesta = curl_exec($ch);
    $codigo    = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $error     = curl_error($ch);
    curl_close($ch);

    // 6) OpenWA responde 201 (o 200) cuando lo aceptó
    $ok = ($error === '' && ($codigo === 201 || $codigo === 200));

    return [
        'ok'      => $ok,
        'mensaje' => $ok ? 'Enviado' : ('OpenWA rechazó el envío (HTTP ' . $codigo . ') ' . $error)
    ];
}
