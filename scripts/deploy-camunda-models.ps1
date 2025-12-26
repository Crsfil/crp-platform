param(
  [string]$CamundaBaseUrl = "http://localhost:18081/engine-rest",
  [string]$ModelsPath = "$PSScriptRoot/../process-models",
  [string]$DeploymentName = "crp-process-models",
  [string]$CamundaUser,
  [string]$CamundaPassword
)

$models = Get-ChildItem -Path $ModelsPath -Recurse -File -Include *.bpmn, *.dmn
if (-not $models) {
  Write-Error "No BPMN/DMN models found under $ModelsPath"
  exit 1
}

$args = @(
  "-s",
  "-X", "POST",
  "$CamundaBaseUrl/deployment/create",
  "-F", "deployment-name=$DeploymentName",
  "-F", "enable-duplicate-filtering=true",
  "-F", "deploy-changed-only=true"
)

if ($CamundaUser -and $CamundaPassword) {
  $args += @("-u", "$CamundaUser`:$CamundaPassword")
}

foreach ($model in $models) {
  $args += @("-F", "data=@$($model.FullName)")
}

Write-Host "[CRP] Deploying $($models.Count) process models to $CamundaBaseUrl"
& curl.exe @args
