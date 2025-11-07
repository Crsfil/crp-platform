param(
  [switch]$Rebuild
)
Set-Location -Path $PSScriptRoot/..
Write-Host "[CRP] Bringing up full stack via Docker Compose..."
if ($Rebuild) {
  docker compose up -d --build
} else {
  docker compose up -d
}
Write-Host "[CRP] Done. Services: gateway http://localhost:8080, auth 8081, inventory 8082, procurement 8083, reports 8084"

