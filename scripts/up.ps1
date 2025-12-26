param(
  [switch]$Rebuild,
  [switch]$WithCamunda
)
Set-Location -Path $PSScriptRoot/..
Write-Host "[CRP] Bringing up stack via Docker Compose..."
$composeFiles = @("docker-compose.yml")
if ($WithCamunda) {
  $composeFiles += "infrastructure/camunda/docker-compose.camunda.yml"
}
$composeArgs = @()
foreach ($file in $composeFiles) {
  $composeArgs += @("-f", $file)
}
if ($Rebuild) {
  docker compose @composeArgs up -d --build
} else {
  docker compose @composeArgs up -d
}
Write-Host "[CRP] Done. Services: gateway http://localhost:8080, auth 8081, inventory 8082, procurement 8083, reports 8084"
