[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$Instance,
    [string]$Jar = (Join-Path $PSScriptRoot '../build/libs/GSTweaks-0.3.1.jar'),
    [switch]$Apply
)

$ErrorActionPreference = 'Stop'
$instanceRoot = (Resolve-Path -LiteralPath $Instance).Path
$kubeRoot = (Resolve-Path -LiteralPath (Join-Path $instanceRoot 'kubejs')).Path
$modsRoot = (Resolve-Path -LiteralPath (Join-Path $instanceRoot 'mods')).Path
$jarSource = (Resolve-Path -LiteralPath $Jar).Path
$jarTarget = Join-Path $modsRoot (Split-Path $jarSource -Leaf)
$manifest = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'files.json') -Raw | ConvertFrom-Json

function Get-Hash([string]$Path) {
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash
}

function Get-InstancePath([string]$Relative) {
    $target = [IO.Path]::GetFullPath((Join-Path $kubeRoot $Relative))
    if (!$target.StartsWith($kubeRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Ongeldig migratiepad $Relative"
    }
    $target
}

$planned = @()
$conflicts = @()
foreach ($entry in $manifest) {
    $target = Get-InstancePath $entry.path
    $exists = Test-Path -LiteralPath $target -PathType Leaf
    $hash = if ($exists) { Get-Hash $target }
    if (!$exists -and !$entry.replacement) { continue }
    if ($exists -and $entry.replacement -and $hash -eq $entry.replacement) { continue }
    if ($exists -and $hash -ne $entry.original -and $hash -ne $entry.previous) {
        $conflicts += $entry.path
        continue
    }
    $overlay = $null
    if ($entry.replacement) {
        $overlay = Join-Path (Join-Path $PSScriptRoot 'kubejs') $entry.path
        if ((Get-Hash $overlay) -ne $entry.replacement) {
            throw "De meegeleverde versie van $($entry.path) komt niet overeen met de manifesthash."
        }
    }
    $planned += [pscustomobject]@{ Relative = $entry.path; Target = $target; Overlay = $overlay; Exists = $exists }
}

$addJar = !(Test-Path -LiteralPath $jarTarget)
if (!$addJar -and (Get-Hash $jarTarget) -ne (Get-Hash $jarSource)) {
    $conflicts += $jarTarget
}
$conflicts += @(Get-ChildItem -LiteralPath $modsRoot -Filter 'GSTweaks-*.jar' -File |
    Where-Object { $_.FullName -ne $jarTarget } | Select-Object -ExpandProperty FullName)

Write-Output "Instance $instanceRoot"
if ($addJar) { Write-Output "Installeren  $(Split-Path $jarTarget -Leaf)" }
foreach ($entry in $planned) {
    if ($entry.Overlay) { Write-Output "Bijwerken   $($entry.Relative)" }
    else { Write-Output "Archiveren  $($entry.Relative)" }
}
if ($conflicts.Count) {
    throw ("Aangepaste bestanden. Voeg deze wijzigingen eerst handmatig samen." + [Environment]::NewLine + ($conflicts -join [Environment]::NewLine))
}
if (!$Apply) {
    Write-Output 'Proefrun. Gebruik -Apply om deze wijzigingen uit te voeren.'
    return
}
if (!$addJar -and !$planned.Count) {
    Write-Output 'De migratie is al toegepast.'
    return
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$backup = Join-Path $instanceRoot "gstweaks-backup/$stamp"
$backupKube = Join-Path $backup 'kubejs'
New-Item -ItemType Directory -Path $backupKube -Force | Out-Null

foreach ($entry in $planned) {
    if ($entry.Exists) {
        $destination = Join-Path $backupKube $entry.Relative
        New-Item -ItemType Directory -Path (Split-Path $destination) -Force | Out-Null
        Copy-Item -LiteralPath $entry.Target -Destination $destination
    }
}

[ordered]@{
    InstalledJar = if ($addJar) { Split-Path $jarTarget -Leaf } else { $null }
    CreatedKubeJSFiles = @($planned | Where-Object { !$_.Exists } | Select-Object -ExpandProperty Relative)
} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backup 'rollback.json')

$processed = [Collections.Generic.List[object]]::new()
$copyingJar = $false
try {
    foreach ($entry in $planned) {
        $processed.Add($entry)
        if ($entry.Overlay) {
            New-Item -ItemType Directory -Path (Split-Path $entry.Target) -Force | Out-Null
            Copy-Item -LiteralPath $entry.Overlay -Destination $entry.Target -Force
        } else {
            Remove-Item -LiteralPath $entry.Target
        }
    }
    if ($addJar) {
        $copyingJar = $true
        Copy-Item -LiteralPath $jarSource -Destination $jarTarget
    }
} catch {
    if ($copyingJar -and (Test-Path -LiteralPath $jarTarget)) {
        Remove-Item -LiteralPath $jarTarget
    }
    foreach ($entry in $processed) {
        if ($entry.Exists) {
            Copy-Item -LiteralPath (Join-Path $backupKube $entry.Relative) -Destination $entry.Target -Force
        } elseif (Test-Path -LiteralPath $entry.Target) {
            Remove-Item -LiteralPath $entry.Target
        }
    }
    throw
}

Write-Output "Klaar. Back-up van oorspronkelijke bestanden in $backup"
