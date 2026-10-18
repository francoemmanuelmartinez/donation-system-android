<#
.SYNOPSIS
    Revisa codificacion de archivos de texto (solo lectura).

.DESCRIPTION
    Detecta problemas que ya rompieron este repo:
      - BOM UTF-8 (EF BB BF): rompe JSON/PHP si va antes de <?php.
      - Caracter de reemplazo U+FFFD (EF BF BD).
      - Mojibake tipico (bytes C3 83; C2 A0/A2; E2 82 AC; C3 B0 C5 B8).
      - Corrupcion historica i->a: byte C3 A1 seguido de 61 / 6F / 61 73 / 6F 73.

    Por defecto escanea los archivos bajo control de version (git ls-files) del repo donde
    vive el script. Con -IncludeUntracked agrega los no versionados (respeta .gitignore).
    Nunca sale de la carpeta del repositorio.

    Sale con codigo 0 si todo esta bien; 1 si hay hallazgos (apto para pre-commit/CI).

.PARAMETER Path
    Carpeta a escanear. Por defecto, la raiz del repo (carpeta padre de scripts\).

.PARAMETER IncludeUntracked
    Incluye tambien archivos no versionados (respeta .gitignore).

.PARAMETER Quiet
    Solo imprime el resumen y el codigo de salida.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\check-encoding.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts\check-encoding.ps1 -IncludeUntracked
#>
[CmdletBinding()]
param(
    [string]$Path,
    [switch]$IncludeUntracked,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'

$repoRoot = if ($Path) { (Resolve-Path $Path).Path } else { Split-Path -Parent $PSScriptRoot }

$skipExt = @('.png','.jpg','.jpeg','.gif','.ico','.webp','.jar','.class','.keystore','.jks','.zip',
             '.gz','.woff','.woff2','.ttf','.otf','.pdf','.apk','.so','.dll','.exe','.bin')

$patterns = [ordered]@{
    'BOM UTF-8'        = [byte[]]@(0xEF,0xBB,0xBF)
    'U+FFFD'           = [byte[]]@(0xEF,0xBF,0xBD)
    'mojibake A-tilde' = [byte[]]@(0xC3,0x83)
    'mojibake nbsp'    = [byte[]]@(0xC2,0xA0)
    'mojibake cent'    = [byte[]]@(0xC2,0xA2)
    'mojibake euro'    = [byte[]]@(0xE2,0x82,0xAC)
    'emoji roto'       = [byte[]]@(0xC3,0xB0,0xC5,0xB8)
    'corrupcion aa'    = [byte[]]@(0xC3,0xA1,0x61)
    'corrupcion aos'   = [byte[]]@(0xC3,0xA1,0x6F,0x73)
    'corrupcion aas'   = [byte[]]@(0xC3,0xA1,0x61,0x73)
    'corrupcion ao'    = [byte[]]@(0xC3,0xA1,0x6F)
}

function Get-FilesToScan {
    param([string]$Root, [switch]$Untracked)
    Push-Location $Root
    try {
        $list = if ($Untracked) { git ls-files --cached --others --exclude-standard } else { git ls-files }
        foreach ($rel in $list) {
            $full = Join-Path $Root $rel
            if (Test-Path -LiteralPath $full -PathType Leaf) { $full }
        }
    } finally { Pop-Location }
}

function Test-Sequence {
    param([byte[]]$Bytes, [byte[]]$Seq)
    for ($i = 0; $i -le $Bytes.Length - $Seq.Length; $i++) {
        $m = $true
        for ($j = 0; $j -lt $Seq.Length; $j++) { if ($Bytes[$i + $j] -ne $Seq[$j]) { $m = $false; break } }
        if ($m) { return $true }
    }
    return $false
}

$files = Get-FilesToScan -Root $repoRoot -Untracked:$IncludeUntracked
$found = 0
$scanned = 0

foreach ($file in $files) {
    if ($skipExt -contains [IO.Path]::GetExtension($file).ToLower()) { continue }
    try { $bytes = [IO.File]::ReadAllBytes($file) } catch { continue }
    if ($bytes.Length -eq 0 -or $bytes.Length -gt 5000000) { continue }
    $scanned++
    $hits = @()
    foreach ($name in $patterns.Keys) { if (Test-Sequence -Bytes $bytes -Seq $patterns[$name]) { $hits += $name } }
    if ($hits.Count -gt 0) {
        $found++
        if (-not $Quiet) { Write-Host ("{0}: {1}" -f $file.Replace($repoRoot + '\', ''), ($hits -join ', ')) -ForegroundColor Yellow }
    }
}

if ($found -eq 0) {
    Write-Host "OK: $scanned archivos sin problemas de codificacion." -ForegroundColor Green
    exit 0
} else {
    Write-Host "ATENCION: $found archivo(s) con problemas (de $scanned escaneados)." -ForegroundColor Red
    exit 1
}
