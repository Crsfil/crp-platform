param(
  [switch]$UseMinikube = $true,
  [string]$MinikubeProfile = "crp",
  [switch]$BuildImages = $true
)

function Require-Cmd($cmd) {
  if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) { throw "Command not found: $cmd" }
}

Require-Cmd kubectl
if ($UseMinikube) { Require-Cmd minikube }
Require-Cmd docker

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot/..

if ($UseMinikube) {
  Write-Host "[CRP] Starting minikube ($MinikubeProfile) and enabling ingress..."
  minikube start -p $MinikubeProfile | Out-Null
  minikube -p $MinikubeProfile addons enable ingress | Out-Null
  if ($BuildImages) {
    Write-Host "[CRP] Pointing Docker to minikube daemon..."
    (& minikube -p $MinikubeProfile docker-env --shell powershell) | Invoke-Expression
  }
}

# Ensure default StorageClass exists
$sc = kubectl get storageclass -o json | ConvertFrom-Json
$hasDefault = $false
foreach ($item in $sc.items) {
  if ($item.metadata.annotations.'storageclass.kubernetes.io/is-default-class' -eq 'true' -or $item.metadata.annotations.'storageclass.beta.kubernetes.io/is-default-class' -eq 'true') { $hasDefault = $true }
}
if (-not $hasDefault) { throw "No default StorageClass found. Please set a default StorageClass before proceeding." }

Write-Host "[CRP] Applying namespace and secrets..."
kubectl apply -f k8s/namespace.yaml | Out-Null
kubectl apply -f k8s/shared-secret.yaml | Out-Null
kubectl apply -f k8s/secrets | Out-Null

Write-Host "[CRP] Applying infra (ZK/Kafka/Redis)..."
kubectl apply -f k8s/infra/zookeeper.yaml | Out-Null
kubectl apply -f k8s/infra/kafka.yaml | Out-Null
kubectl apply -f k8s/infra/redis.yaml | Out-Null

Write-Host "[CRP] Applying Postgres StatefulSets..."
kubectl apply -f k8s/postgres | Out-Null
$dbSets = @(
  'auth-postgres','inventory-postgres','procurement-postgres','reports-postgres',
  'customer-postgres','application-postgres','agreement-postgres','billing-postgres','schedule-postgres','accounting-postgres'
)
foreach ($s in $dbSets) {
  Write-Host "[CRP] Waiting for $s..."
  kubectl rollout status statefulset/$s -n crp --timeout=180s | Out-Null
}

if ($BuildImages) {
  Write-Host "[CRP] Building service images..."
  $services = @(
    'auth-service','inventory-service','procurement-service','reports-service',
    'customer-service','kyc-service','underwriting-service','product-pricing-service',
    'application-service','agreement-service','schedule-service','billing-service',
    'payments-service','accounting-service','gateway-service','bpm-service'
  )
  foreach ($svc in $services) {
    $tag = "crp-$svc:dev"
    Write-Host "[CRP] Building $tag ..."
    docker build -t $tag -f "$svc/Dockerfile" . | Out-Null
  }
}

Write-Host "[CRP] Applying application deployments..."
kubectl apply -f k8s/auth-service/deployment.yaml | Out-Null
kubectl apply -f k8s/inventory-service/deployment.yaml | Out-Null
kubectl apply -f k8s/procurement-service/deployment.yaml | Out-Null
kubectl apply -f k8s/reports-service/deployment.yaml | Out-Null
kubectl apply -f k8s/customer-service/deployment.yaml | Out-Null
kubectl apply -f k8s/kyc-service/deployment.yaml | Out-Null
kubectl apply -f k8s/underwriting-service/deployment.yaml | Out-Null
kubectl apply -f k8s/product-pricing-service/deployment.yaml | Out-Null
kubectl apply -f k8s/application-service/deployment.yaml | Out-Null
kubectl apply -f k8s/agreement-service/deployment.yaml | Out-Null
kubectl apply -f k8s/schedule-service/deployment.yaml | Out-Null
kubectl apply -f k8s/billing-service/deployment.yaml | Out-Null
kubectl apply -f k8s/payments-service/deployment.yaml | Out-Null
kubectl apply -f k8s/accounting-service/deployment.yaml | Out-Null
kubectl apply -f k8s/gateway-service/deployment.yaml | Out-Null

if ($BuildImages) {
  Write-Host "[CRP] Patching deployments to use locally built images..."
  $map = @{
    'auth-service'='crp-auth-service:dev';
    'inventory-service'='crp-inventory-service:dev';
    'procurement-service'='crp-procurement-service:dev';
    'reports-service'='crp-reports-service:dev';
    'customer-service'='crp-customer-service:dev';
    'kyc-service'='crp-kyc-service:dev';
    'underwriting-service'='crp-underwriting-service:dev';
    'product-pricing-service'='crp-product-pricing-service:dev';
    'application-service'='crp-application-service:dev';
    'agreement-service'='crp-agreement-service:dev';
    'schedule-service'='crp-schedule-service:dev';
    'billing-service'='crp-billing-service:dev';
    'payments-service'='crp-payments-service:dev';
    'accounting-service'='crp-accounting-service:dev';
    'gateway-service'='crp-gateway-service:dev';
    'bpm-service'='crp-bpm-service:dev'
  }
  foreach ($k in $map.Keys) {
    kubectl set image deploy/$k $k=$($map[$k]) -n crp | Out-Null
  }
}

Write-Host "[CRP] Waiting for gateway to be ready..."
kubectl rollout status deploy/gateway-service -n crp --timeout=180s | Out-Null

if ($UseMinikube) {
  Write-Host "[CRP] Applying ingress and printing URL..."
  kubectl apply -f k8s/ingress.yaml | Out-Null
  $ip = (kubectl get svc -n ingress-nginx -l app.kubernetes.io/component=controller -o jsonpath='{.items[0].status.loadBalancer.ingress[0].ip}');
  if (-not $ip) { $ip = (minikube -p $MinikubeProfile ip) }
  Write-Host "[CRP] Gateway likely reachable via http://crp.local (map to $ip)"
  Write-Host "[CRP] Or port-forward: kubectl port-forward deploy/gateway-service -n crp 8080:8080"
}

Write-Host "[CRP] Done. kubectl get pods -n crp"

