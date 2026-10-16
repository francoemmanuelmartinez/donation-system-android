<#
.SYNOPSIS
    Libera el puerto 3306 para MariaDB de XAMPP y arranca el servidor.

.DESCRIPTION
    En la notebook (PARDO-NET) el servicio de Windows MySQL97 (MySQL Server 9.7)
    ocupa el puerto 3306 y exige contrasena de root, con lo que la API de XAMPP
    (root sin contrasena) responde 500 "Access denied". Este script:

      1. Detiene el servicio MySQL97 y lo deja en arranque Manual (reversible).
      2. Arranca MariaDB de XAMPP (mysqld --defaults-file=...\my.ini).
      3. Espera a que `mysqladmin -u root ping` responda "mysqld is alive".

    Con -Restore revierte todo: detiene MariaDB de XAMPP, vuelve MySQL97 a
    Automatic y lo arranca.

    Requiere permisos de administrador (se auto-eleva mostrando el UAC).

.PARAMETER Restore
    Revierte los cambios y deja el entorno como estaba (MySQL97 activo).

.PARAMETER Xampp
    Ruta de instalacion de XAMPP. Por defecto C:\xampp.

.PARAMETER MySqlService
    Nombre del servicio de MySQL que ocupa el 3306. Por defecto MySQL97.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\start-xampp-mysql.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\start-xampp-mysql.ps1 -Restore
#>
[CmdletBinding()]
param(
    [switch]$Restore,
    [string]$Xampp = 'C:\xampp',
    [string]$MySqlService = 'MySQL97'
)

$ErrorActionPreference = 'Stop'

function Test-IsAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    (New-Object Security.Principal.WindowsPrincipal($id)).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)
}

# --- Auto-elevacion ---------------------------------------------------------
if (-not (Test-IsAdmin)) {
    Write-Host 'Se requieren permisos de administrador. Abriendo UAC...' -ForegroundColor Yellow
    $psExe = (Get-Process -Id $PID).Path
    $argList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"")
    if ($Restore) { $argList += '-Restore' }
    Start-Process -FilePath $psExe -Verb RunAs -Wait -ArgumentList $argList
    exit
}

$mysqlBin   = Join-Path $Xampp 'mysql\bin'
$mysqld     = Join-Path $mysqlBin 'mysqld.exe'
$mysql      = Join-Path $mysqlBin 'mysql.exe'
$mysqlAdmin = Join-Path $mysqlBin 'mysqladmin.exe'
$myIni      = Join-Path $mysqlBin 'my.ini'
$errorLog   = Join-Path $Xampp 'mysql\data\mysql_error.log'

foreach ($f in @($mysqld, $mysql, $mysqlAdmin, $myIni)) {
    if (-not (Test-Path $f)) { throw "No se encontro: $f  (ajusta -Xampp)" }
}

function Test-MariaDbAlive {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    & $mysql -u root -e 'SELECT 1' 2>&1 | Out-Null
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return ($code -eq 0)
}

function Get-Port3306Owner {
    Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty OwningProcess
}

# --- Modo restaurar ---------------------------------------------------------
if ($Restore) {
    Write-Host '=== Restaurando estado original ===' -ForegroundColor Cyan

    if (Test-MariaDbAlive) {
        Write-Host 'Deteniendo MariaDB de XAMPP (mysqladmin shutdown)...'
        $prev = $ErrorActionPreference; $ErrorActionPreference = 'SilentlyContinue'
        & $mysqlAdmin -u root shutdown 2>&1 | Out-Null
        $ErrorActionPreference = $prev
        Start-Sleep -Seconds 2
    }

    $owner = Get-Port3306Owner
    if ($owner) {
        $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$owner" -ErrorAction SilentlyContinue
        if ($proc -and $proc.ExecutablePath -like "$Xampp*") {
            Write-Host "Deteniendo mysqld de XAMPP (PID $owner)..."
            Stop-Process -Id $owner -Force
        }
    }

    Write-Host "Dejando el servicio $MySqlService en Automatic e iniciandolo..."
    Set-Service -Name $MySqlService -StartupType Automatic
    Start-Service -Name $MySqlService
    Write-Host 'Entorno restaurado (MySQL97 activo).' -ForegroundColor Green
    exit 0
}

# --- Modo normal: liberar 3306 y arrancar MariaDB ---------------------------
Write-Host '=== Liberando el puerto 3306 ===' -ForegroundColor Cyan

$svc = Get-Service -Name $MySqlService -ErrorAction SilentlyContinue
if ($svc) {
    if ($svc.Status -ne 'Stopped') {
        Write-Host "Deteniendo el servicio $MySqlService..."
        Stop-Service -Name $MySqlService -Force
    }
    if ($svc.StartType -ne 'Manual') {
        Write-Host "Dejando el servicio $MySqlService en arranque Manual..."
        Set-Service -Name $MySqlService -StartupType Manual
    }
} else {
    Write-Host "Aviso: no existe el servicio '$MySqlService' (continuo)." -ForegroundColor Yellow
}

$waited = 0
while ((Get-Port3306Owner) -and $waited -lt 15) { Start-Sleep -Seconds 1; $waited++ }

if (Test-MariaDbAlive) {
    Write-Host 'MariaDB de XAMPP ya estaba en ejecucion.' -ForegroundColor Green
    exit 0
}

Write-Host 'Arrancando MariaDB de XAMPP...'
Start-Process -FilePath $mysqld -ArgumentList "--defaults-file=`"$myIni`"" -WindowStyle Hidden

$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    if (Test-MariaDbAlive) { break }
    Start-Sleep -Seconds 2
}

if (Test-MariaDbAlive) {
    Write-Host 'MariaDB de XAMPP respondiendo (mysqld is alive).' -ForegroundColor Green
    exit 0
}

Write-Host 'ERROR: MariaDB no respondio. Ultimas lineas del log:' -ForegroundColor Red
if (Test-Path $errorLog) { Get-Content $errorLog -Tail 15 }
exit 1
