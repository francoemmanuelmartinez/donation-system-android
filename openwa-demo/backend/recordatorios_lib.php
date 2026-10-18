<?php
// openwa-demo/backend/recordatorios_lib.php
//
// Conexión a la base de datos, búsqueda de pendientes y marca de enviados.
require_once __DIR__ . '/openwa_config.php';

// Conexión a MySQL (PDO)
function conectar() {
    $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4';
    return new PDO($dsn, DB_USER, DB_PASS, [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);
}

// Trae los recordatorios que ya toca enviar (los de hoy o antes, no enviados)
function buscarPendientes($conn) {
    // TODO: si usás otra tabla, cambiá "recordatorios" y los nombres de columnas.
    $sql = "SELECT id, destinatario_nombre, telefono, mensaje, programado_para, fecha_envio
              FROM recordatorios
             WHERE enviado = 0
               AND (
                 (programado_para IS NOT NULL AND programado_para <= NOW())
                 OR (programado_para IS NULL AND fecha_envio <= CURDATE())
               )
          ORDER BY COALESCE(programado_para, TIMESTAMP(fecha_envio)) ASC";
    return $conn->query($sql)->fetchAll();
}

// Crea un recordatorio con programado_para (delay en minutos)
function crearRecordatorio($conn, $nombre, $telefono, $mensaje, $programadoPara) {
    $stmt = $conn->prepare("
        INSERT INTO recordatorios (destinatario_nombre, telefono, mensaje, fecha_envio, programado_para, enviado)
        VALUES (:nombre, :telefono, :mensaje, CURDATE(), :programado_para, 0)
    ");
    $stmt->execute([
        'nombre'          => $nombre,
        'telefono'        => $telefono,
        'mensaje'         => $mensaje,
        'programado_para' => $programadoPara
    ]);
    return $conn->lastInsertId();
}

// Escribe una línea en el log (sirve para entender qué pasó)
function escribirLog($mensaje) {
    if (!is_dir(dirname(LOG_PATH))) {
        mkdir(dirname(LOG_PATH), 0777, true);
    }
    $linea = '[' . date('Y-m-d H:i:s') . '] ' . $mensaje . PHP_EOL;
    file_put_contents(LOG_PATH, $linea, FILE_APPEND | LOCK_EX);
}

// Marca un recordatorio como enviado (para no reenviarlo)
function marcarEnviado($conn, $id) {
    $stmt = $conn->prepare("UPDATE recordatorios SET enviado = 1, enviado_en = NOW() WHERE id = :id");
    $stmt->execute(['id' => $id]);
}
