<#
.SYNOPSIS
    Sincroniza la base 'donation_system' de XAMPP con el snapshot SQL del repo.

.DESCRIPTION
    Puente entre la base real que usa XAMPP y los archivos de /sql versionados
    en el repositorio. Pensado para correr en la PC de escritorio y en la
    notebook (ver la tabla de rutas por dispositivo en AGENTS.md).

      -Push  : recrea la base (DROP + CREATE) e importa sql\donation_system.sql.
               Es la accion por defecto si no se indica ninguna.
      -Seed  : importa sql\seed_40_donantes_turnos.sql (datos de demo). Es de
               un solo uso: los DNI son UNIQUE, reimportarlo falla por duplicados.
               Combinado con -Push queda un entorno limpio con datos de demo.
      -Pull  : vuelca la base real a sql\donation_system.sql (mysqldump).

    Requiere que MariaDB de XAMPP este corriendo en localhost:3306.
    Si no responde, correr primero scripts\start-xampp-mysql.ps1.

.PARAMETER Push
    Recrea la base desde el snapshot del repo.

.PARAMETER Seed
    Carga los datos de demo (sql\seed_40_donantes_turnos.sql).

.PARAMETER Pull
    Volca la base real al snapshot del repo (sobrescribe sql\donation_system.sql).

.PARAMETER Database
    Nombre de la base. Por defecto donation_system.

.PARAMETER MysqlBin
    Carpeta con mysql.exe / mysqldump.exe / mysqladmin.exe.
    Por defecto C:\xampp\mysql\bin.

.PARAMETER Force
    Permite operar sobre 'donation_system_test' (peligroso; ver TESTING.md).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\sync-db.ps1 -Push -Seed

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\sync-db.ps1 -Pull
#>
[CmdletBinding()]
param(
    [switch]$Push,
    [switch]$Seed,
    [switch]$Pull,
    [string]$Database = 'donation_system',
    [string]$MysqlBin = 'C:\xampp\mysql\bin',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

if (-not ($Push -or $Seed -or $Pull)) { $Push = $true }

$repoRoot   = Split-Path -Parent $PSScriptRoot
$schemaFile = Join-Path $repoRoot 'sql\donation_system.sql'
$seedFile   = Join-Path $repoRoot 'sql\seed_40_donantes_turnos.sql'

$mysql      = Join-Path $MysqlBin 'mysql.exe'
$mysqldump  = Join-Path $MysqlBin 'mysqldump.exe'
$mysqlAdmin = Join-Path $MysqlBin 'mysqladmin.exe'

foreach ($f in @($mysql, $mysqldump, $mysqlAdmin)) {
    if (-not (Test-Path $f)) { throw "No se encontro: $f  (ajusta -MysqlBin)" }
}

# --- Guarda: nunca pisar la base de tests sin -Force ------------------------
if ($Database -eq 'donation_system_test' -and -not $Force) {
    throw "Se intento operar sobre 'donation_system_test' sin -Force. " +
          "Ver TESTING.md: esa base la recrea la suite de tests y no debe pisarse por error."
}

function Test-MariaDbAlive {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    & $mysql -u root -e 'SELECT 1' 2>&1 | Out-Null
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return ($code -eq 0)
}

function Assert-MariaDb {
    if (-not (Test-MariaDbAlive)) {
        throw 'MariaDB no responde en localhost:3306. Ejecuta primero scripts\start-xampp-mysql.ps1.'
    }
}

function Invoke-SqlFile {
    param([string]$Path, [string]$Db)
    if (-not (Test-Path $Path)) { throw "No existe el archivo SQL: $Path" }
    Write-Host "  -> importando $(Split-Path -Leaf $Path)"
    $p = Start-Process -FilePath $mysql -NoNewWindow -Wait -PassThru `
         -ArgumentList @('-u', 'root', '--default-character-set=utf8mb4', $Db) `
         -RedirectStandardInput $Path
    if ($p.ExitCode -ne 0) { throw "Fallo la importacion de $Path (exit $($p.ExitCode))" }
}

Assert-MariaDb

if ($Push) {
    Write-Host "=== PUSH: recreando la base '$Database' desde el snapshot ===" -ForegroundColor Cyan
    $sql = "DROP DATABASE IF EXISTS $Database; CREATE DATABASE $Database CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    & $mysql -u root -e $sql
    if ($LASTEXITCODE -ne 0) { throw "No se pudo recrear la base '$Database'" }
    Invoke-SqlFile -Path $schemaFile -Db $Database
    Write-Host "Base '$Database' recreada desde sql\donation_system.sql" -ForegroundColor Green
}

if ($Seed) {
    Write-Host '=== SEED: cargando datos de demo ===' -ForegroundColor Cyan
    Invoke-SqlFile -Path $seedFile -Db $Database
    Write-Host 'Seed cargado.' -ForegroundColor Green
}

if ($Pull) {
    Write-Host "=== PULL: volcando '$Database' a sql\donation_system.sql ===" -ForegroundColor Cyan
    $p = Start-Process -FilePath $mysqldump -NoNewWindow -Wait -PassThru `
         -ArgumentList @('-u', 'root', '--default-character-set=utf8mb4', '--add-drop-table', $Database) `
         -RedirectStandardOutput $schemaFile
    if ($p.ExitCode -ne 0) { throw "Fallo el mysqldump (exit $($p.ExitCode))" }
    Write-Host 'Snapshot actualizado: sql\donation_system.sql' -ForegroundColor Green
}

Write-Host 'Listo.' -ForegroundColor Green
