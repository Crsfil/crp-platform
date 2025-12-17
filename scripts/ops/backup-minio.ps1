param(
    [string]$OutputDir = "backups\\minio",
    [string]$Network = "crp-platform_default"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$dest = Join-Path $OutputDir $timestamp
New-Item -ItemType Directory -Path $dest -Force | Out-Null
$abs = Resolve-Path $dest

Write-Host "Backup MinIO buckets -> $abs"

$mcImage = "minio/mc:RELEASE.2024-06-12T14-34-03Z"
$cmd = @(
    "mc alias set local http://minio:9000 minio minio12345",
    "mc mirror local/crp-reports /backup/crp-reports",
    "mc mirror local/crp-procurement /backup/crp-procurement",
    "mc mirror local/crp-inventory /backup/crp-inventory"
) -join " && "

docker run --rm --network $Network -v "$($abs.Path):/backup" $mcImage /bin/sh -c $cmd

if ($LASTEXITCODE -ne 0) {
    throw "MinIO backup failed"
}

Write-Host "MinIO backup complete."
