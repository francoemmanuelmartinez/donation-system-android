<?php
// whatsapp_sender.php
// Adaptador único de envío de WhatsApp.
// Proveedores: 'openwa' (gateway local, gratis) y 'meta' (Cloud API oficial, pago por mensaje).
// El activo lo elige REMINDER_PROVIDER en config_reminders.php. Modo dual: ambos conviven y
// la conmutación es de config (<1 min; ver whatsapp-reminders-guide.md §7.12).
// sendWhatsappText() recibe $params (variables de la plantilla) que usa sendViaMeta();
// sendViaOpenWa() los ignora y envía el texto libre.

require_once __DIR__ . '/db_config.php';
if (file_exists(__DIR__ . '/config_reminders.php')) {
    require_once __DIR__ . '/config_reminders.php';
}

/**
 * Envía un texto por WhatsApp a un número.
 *
 * @param string $number Número con prefijo de país (ej: 541167452580)
 * @param string $text   Texto a enviar (lo usa OpenWA)
 * @param array|null $params Variables de la plantilla, en orden {{1}},{{2}},... (lo usa Meta)
 * @return array ['ok' => bool, 'message' => string, 'raw' => mixed]
 */
function sendWhatsappText($number, $text, $params = null) {
    if (!defined('REMINDER_PROVIDER') || REMINDER_PROVIDER === 'openwa') {
        return sendViaOpenWa($number, $text);
    }
    if (REMINDER_PROVIDER === 'meta') {
        return sendViaMeta($number, $text, $params);
    }
    return ['ok' => false, 'message' => 'Proveedor de WhatsApp desconocido', 'raw' => null];
}

/**
 * Normaliza un número de teléfono a formato con prefijo de país.
 * Misma regla que usa la app: si tiene 10-12 dígitos y no arranca con "54",
 * antepone "54" (código de país de Argentina).
 */
function normalizeWaNumber($phoneDigits) {
    $phoneDigits = preg_replace('/[^0-9]/', '', (string)$phoneDigits);
    if ($phoneDigits === '') {
        return '';
    }
    if (strlen($phoneDigits) >= 10 && strlen($phoneDigits) <= 12 && strpos($phoneDigits, '54') !== 0) {
        return '54' . $phoneDigits;
    }
    return $phoneDigits;
}

// ---------------------------------------------------------------------------
// OpenWA
// ---------------------------------------------------------------------------

function sendViaOpenWa($number, $text) {
    if (!function_exists('curl_init')) {
        return ['ok' => false, 'message' => 'curl no está disponible en este servidor', 'raw' => null];
    }

    if (!defined('OPENWA_SESSION_ID') || OPENWA_SESSION_ID === '') {
        return ['ok' => false, 'message' => 'OPENWA_SESSION_ID no está configurado', 'raw' => null];
    }

    $url = rtrim(OPENWA_BASE_URL, '/') . '/api/sessions/' . OPENWA_SESSION_ID . '/messages/send-text';

    $payload = json_encode([
        'chatId' => $number . '@c.us',
        'text'   => $text
    ], JSON_UNESCAPED_UNICODE);

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => $payload,
        CURLOPT_HTTPHEADER     => [
            'Content-Type: application/json',
            'X-API-Key: ' . OPENWA_API_KEY
        ],
        CURLOPT_TIMEOUT        => 20,
        CURLOPT_CONNECTTIMEOUT => 5
    ]);

    $response   = curl_exec($ch);
    $httpCode   = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError  = curl_error($ch);
    curl_close($ch);

    $decoded = $response ? json_decode($response, true) : null;
    $ok      = $curlError === '' && ($httpCode === 201 || $httpCode === 200);

    return [
        'ok'      => $ok,
        'message' => $ok
            ? 'Enviado'
            : 'OpenWA rechazó (HTTP ' . $httpCode . ')'
              . ($curlError !== '' ? ' - ' . $curlError : '')
              . ($decoded !== null ? ' - ' . json_encode($decoded, JSON_UNESCAPED_UNICODE) : ''),
        'raw'     => $decoded !== null ? $decoded : $response
    ];
}

// ---------------------------------------------------------------------------
// API oficial de Meta (Cloud API) - IMPLEMENTADO (Fase 2)
// Requiere completar META_TOKEN / META_PHONE_NUMBER_ID en config_reminders.php
// y una plantilla UTILITY aprobada (META_TEMPLATE_NAME). El sandbox/test number
// permite validar todo gratis antes de activar producción.
// ---------------------------------------------------------------------------

function sendViaMeta($number, $text, $params = null) {
    if (!function_exists('curl_init')) {
        return ['ok' => false, 'message' => 'curl no está disponible en este servidor', 'raw' => null];
    }

    if (!defined('META_TOKEN') || META_TOKEN === '') {
        return ['ok' => false, 'message' => 'META_TOKEN no configurado (Fase 2 lista; falta la credencial)', 'raw' => null];
    }
    if (!defined('META_PHONE_NUMBER_ID') || META_PHONE_NUMBER_ID === '') {
        return ['ok' => false, 'message' => 'META_PHONE_NUMBER_ID no configurado', 'raw' => null];
    }
    if (!defined('META_TEMPLATE_NAME') || META_TEMPLATE_NAME === '') {
        return ['ok' => false, 'message' => 'META_TEMPLATE_NAME no configurado', 'raw' => null];
    }

    $parameters = buildMetaBodyParameters($params, $text);
    if ($parameters === null) {
        return ['ok' => false, 'message' => 'Faltan parámetros para la plantilla de Meta (variables vacías)', 'raw' => null];
    }

    $language = defined('META_TEMPLATE_LANGUAGE') && META_TEMPLATE_LANGUAGE !== ''
        ? META_TEMPLATE_LANGUAGE
        : 'es_AR';

    $payload = json_encode([
        'messaging_product' => 'whatsapp',
        'to'                 => $number,
        'type'               => 'template',
        'template'           => [
            'name'       => META_TEMPLATE_NAME,
            'language'   => ['code' => $language],
            'components' => [
                ['type' => 'body', 'parameters' => $parameters]
            ]
        ]
    ], JSON_UNESCAPED_UNICODE);

    $apiVersion = defined('META_API_VERSION') && META_API_VERSION !== '' ? META_API_VERSION : 'v21.0';
    $url = 'https://graph.facebook.com/' . $apiVersion . '/' . META_PHONE_NUMBER_ID . '/messages';

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => $payload,
        CURLOPT_HTTPHEADER     => [
            'Content-Type: application/json',
            'Authorization: Bearer ' . META_TOKEN
        ],
        CURLOPT_TIMEOUT        => 20,
        CURLOPT_CONNECTTIMEOUT => 5
    ]);
    $response  = curl_exec($ch);
    $httpCode  = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    $decoded = $response ? json_decode($response, true) : null;
    $ok      = $curlError === '' && $httpCode >= 200 && $httpCode < 300;

    $msg = 'Enviado vía Meta';
    if (!$ok) {
        $msg = 'Error HTTP ' . $httpCode;
        if ($decoded !== null && isset($decoded['error']['message'])) {
            $msg .= ' - ' . $decoded['error']['message'];
        } elseif ($curlError !== '') {
            $msg .= ' - ' . $curlError;
        }
    }

    return [
        'ok'      => $ok,
        'message' => $msg,
        'raw'     => $decoded !== null ? $decoded : $response
    ];
}

/**
 * Arma los parámetros posicionales del body de la plantilla.
 * - Si $params es un array ordenado ({{1}},{{2}},...) lo usa tal cual; rechaza variables vacías.
 * - Si no hay $params pero sí $text, usa $text como única variable (útil con templates de 1 variable).
 * @return array|null null si hay variables vacías o no hay nada para enviar.
 */
function buildMetaBodyParameters($params, $text) {
    if (is_array($params) && count($params) > 0) {
        $values = array_values($params);
        $clean  = array_filter(array_map('trim', array_map('strval', $values)), 'strlen');
        if (count($clean) !== count($values)) {
            return null;
        }
        $result = [];
        foreach ($values as $v) {
            $result[] = ['type' => 'text', 'text' => (string)$v];
        }
        return $result;
    }
    if (trim((string)$text) !== '') {
        return [['type' => 'text', 'text' => (string)$text]];
    }
    return null;
}
?>