# Generate the offline payload manifest (assets/payloads/targets-v3.json)
# from the authoritative upstream manifest (support/targets-v3.json of
# BuSung-dev/Root-My-Galaxy-Payloads).
#
# This script is a converter, not a second device database: every entry of the
# upstream manifest is preserved as-is (payloadId, displayName, models,
# kernelVersions, requiresFreshP0Session and any future optional fields).
# Only the artifact URLs are rewritten to bundled-asset URLs and their sizes
# are re-measured from the local files.
#
# Usage:
#   1. Download the upstream manifest:
#      https://raw.githubusercontent.com/BuSung-dev/Root-My-Galaxy-Payloads/main/support/targets-v3.json
#      and save it next to this script as upstream-targets-v3.json
#   2. Place payload artifacts under app/src/main/assets/payloads/:
#      - per-device exploit:  payloads/<payloadId>/cve-2026-43499-app.so
#      - shared ksud builds:  payloads/ksud/<ksud filename from upstream URL>
#   3. Run:  powershell -ExecutionPolicy Bypass -File gen-offline-manifest.ps1
#
# The script fails loudly when a referenced artifact is missing, instead of
# silently producing a manifest that would fail at runtime.

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$base = Join-Path $root "app\src\main\assets\payloads"
$upstreamManifestPath = Join-Path $root "upstream-targets-v3.json"
$out = Join-Path $base "targets-v3.json"

if (-not (Test-Path $upstreamManifestPath)) {
    throw "Upstream manifest not found: $upstreamManifestPath`nDownload it from https://raw.githubusercontent.com/BuSung-dev/Root-My-Galaxy-Payloads/main/support/targets-v3.json"
}

$upstream = Get-Content $upstreamManifestPath -Raw | ConvertFrom-Json
if ($upstream.schemaVersion -ne 3) {
    throw "Unsupported upstream schemaVersion: $($upstream.schemaVersion) (expected 3)"
}

function Resolve-Asset([string]$url, [string]$payloadId) {
    # Upstream artifact URL -> local bundled asset path.
    # Current upstream formats (support/targets-v3.json on main):
    #   .../artifacts/<payloadId>/cve-2026-43499-app.so
    #   .../kernelsu/ksud-<name>
    # The app-bundled flavor (historical app manifests) uses:
    #   assets://payloads/<payloadId>/cve-2026-43499-app.so
    #   assets://payloads/ksud/ksud-<name>
    # Both are recognized so either manifest source converts cleanly.
    if ($url -match '(?:kernelsu|ksud)/(ksud[^/]*)$') {
        return @{ dir = "ksud"; file = $Matches[1] }
    }
    if ($url -match '(?:artifacts|payloads)/([^/]+)/cve-2026-43499-app\.so$') {
        return @{ dir = $Matches[1]; file = "cve-2026-43499-app.so" }
    }
    throw "Unrecognized artifact URL for ${payloadId}: $url"
}

$entries = New-Object System.Collections.Generic.List[object]
foreach ($p in $upstream.payloads) {
    $exploitAsset = Resolve-Asset $p.exploit.url $p.payloadId
    $ksudAsset = Resolve-Asset $p.kernelsu.url $p.payloadId

    $exploitPath = Join-Path $base "$($exploitAsset.dir)\$($exploitAsset.file)"
    $ksudPath = Join-Path $base "$($ksudAsset.dir)\$($ksudAsset.file)"
    if (-not (Test-Path $exploitPath)) {
        throw "Missing bundled exploit for $($p.payloadId): $exploitPath"
    }
    if (-not (Test-Path $ksudPath)) {
        throw "Missing bundled ksud for $($p.payloadId): $ksudPath"
    }

    # Clone the original entry and touch only the artifact fields, so optional
    # upstream metadata (requiresFreshP0Session, future fields) survives.
    $entry = $p.PSObject.Copy()
    $entry.exploit.url = "asset://payloads/$($exploitAsset.dir)/$($exploitAsset.file)"
    $entry.exploit.size = (Get-Item $exploitPath).Length
    $entry.kernelsu.url = "asset://payloads/$($ksudAsset.dir)/$($ksudAsset.file)"
    $entry.kernelsu.size = (Get-Item $ksudPath).Length
    $entries.Add($entry)
}

$manifest = [ordered]@{ schemaVersion = 3; payloads = $entries }
$json = $manifest | ConvertTo-Json -Depth 8
[System.IO.File]::WriteAllText($out, $json, [System.Text.UTF8Encoding]::new($false))
Write-Host "WROTE $out with $($entries.Count) entries"
