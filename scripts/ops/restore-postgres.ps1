param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [Parameter(Mandatory = $true)][string]$Container,
    [Parameter(Mandatory = $true)][string]$Database,
    [Parameter(Mandatory = $true)][string]$User
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path $BackupFile)) {
    throw "Backup file not found: $BackupFile"
}

Write-Host "Restore $BackupFile -> $Container/$Database"
Get-Content -Path $BackupFile | docker exec -i $Container psql -U $User -d $Database

if ($LASTEXITCODE -ne 0) {
    throw "psql restore failed for $Container/$Database"
}

Write-Host "Restore complete."
