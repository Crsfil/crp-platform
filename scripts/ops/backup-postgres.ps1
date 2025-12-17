param(
    [string]$OutputDir = "backups\\pg"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$targets = @(
    @{ Container = "inventory-postgres"; Db = "inventorydb"; User = "inventory" },
    @{ Container = "procurement-postgres"; Db = "procurementdb"; User = "proc" },
    @{ Container = "reports-postgres"; Db = "reportsdb"; User = "reports" },
    @{ Container = "customer-postgres"; Db = "customerdb"; User = "customer" },
    @{ Container = "application-postgres"; Db = "applicationdb"; User = "app" },
    @{ Container = "agreement-postgres"; Db = "agreementdb"; User = "agreement" },
    @{ Container = "billing-postgres"; Db = "billingdb"; User = "billing" },
    @{ Container = "schedule-postgres"; Db = "scheduledb"; User = "schedule" },
    @{ Container = "accounting-postgres"; Db = "accountingdb"; User = "accounting" }
)

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

foreach ($t in $targets) {
    $file = Join-Path $OutputDir "$($t.Container)-$($t.Db)-$timestamp.sql"
    Write-Host "Backup $($t.Container)/$($t.Db) -> $file"
    docker exec $t.Container pg_dump -U $t.User -d $t.Db | Out-File -FilePath $file -Encoding utf8
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump failed for $($t.Container)/$($t.Db)"
    }
}

Write-Host "Backups complete."
