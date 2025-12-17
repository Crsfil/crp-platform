param(
    [string]$KeycloakUrl = "http://localhost:18080",
    [string]$InventoryUrl = "http://localhost:8082",
    [string]$ProcurementUrl = "http://localhost:8083",
    [string]$Username = "admin@crp.local",
    [string]$Password = "admin",
    [string]$ClientId = "crp-cli"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Wait-Http {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [int]$TimeoutSec = 180
    )
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSec)
    while ([DateTime]::UtcNow -lt $deadline) {
        try {
            Invoke-RestMethod -Method Get -Uri $Url -TimeoutSec 5 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 3
        }
    }
    throw "Timeout waiting for $Url"
}

function Get-AccessToken {
    param(
        [string]$BaseUrl,
        [string]$User,
        [string]$Pass,
        [string]$Client
    )
    $tokenResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/realms/crp/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body @{
            client_id = $Client
            username  = $User
            password  = $Pass
            grant_type = "password"
        }
    return $tokenResponse.access_token
}

function Invoke-Json {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = $null
    )
    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
        ErrorAction = "Stop"
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 6)
    }
    try {
        return Invoke-RestMethod @params
    } catch {
        if ($null -ne $_.Exception.Response) {
            $status = $_.Exception.Response.StatusCode.value__
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $bodyText = $reader.ReadToEnd()
            Write-Host "HTTP $status for $Method $Url"
            if ($bodyText) {
                Write-Host $bodyText
            }
        }
        throw
    }
}

$correlationId = [guid]::NewGuid().ToString()

Write-Host "Waiting for Keycloak, Inventory, Procurement..."
Wait-Http "$KeycloakUrl/realms/crp/.well-known/openid-configuration" 240
Wait-Http "$InventoryUrl/actuator/health" 240
Wait-Http "$ProcurementUrl/actuator/health" 240

$token = Get-AccessToken -BaseUrl $KeycloakUrl -User $Username -Pass $Password -Client $ClientId
$headers = @{
    Authorization = "Bearer $token"
    "X-Correlation-Id" = $correlationId
}

Write-Host "Create location..."
$locCode = "MSK-" + (Get-Random -Minimum 1000 -Maximum 9999)
$location = Invoke-Json -Method Post -Url "$InventoryUrl/locations" -Headers $headers -Body @{
    code = $locCode
    name = "Moscow Storage $locCode"
    parentId = $null
    type = "WAREHOUSE"
    address = "Moscow"
    region = "msk"
}
$locationId = $location.id

Write-Host "Create equipment..."
$equipment = Invoke-Json -Method Post -Url "$InventoryUrl/equipment" -Headers $headers -Body @{
    type = "EXCAVATOR"
    manufacturer = "CAT"
    model = "320D"
    condition = "USED"
    price = 1200000
    locationId = $locationId
    serialNumber = "SN-$locCode"
    branchCode = $locCode
}
$equipmentId = $equipment.id

Write-Host "Open repossession case..."
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/$equipmentId/repossessions" -Headers $headers -Body @{
    triggerReason = "overdue"
    decisionRef = "DEC-$locCode"
    targetLocationId = $locationId
} | Out-Null

$storageSla = (Get-Date).AddDays(5).ToString("o")
Write-Host "Create storage order..."
$storageOrder = Invoke-Json -Method Post -Url "$InventoryUrl/equipment/$equipmentId/storage-orders" -Headers $headers -Body @{
    storageLocationId = $locationId
    vendorName = "Storage LLC"
    vendorInn = "7701234567"
    slaUntil = $storageSla
    expectedCost = 50000
    currency = "RUB"
    note = "intake"
}
$storageOrderId = $storageOrder.id

Write-Host "Create procurement service order..."
$serviceOrder = Invoke-Json -Method Post -Url "$ProcurementUrl/service/orders" -Headers $headers -Body @{
    serviceType = "SERVICE_STORAGE"
    equipmentId = $equipmentId
    locationId = $locationId
    supplierId = 1
    vendorName = "Storage LLC"
    vendorInn = "7701234567"
    slaUntil = $storageSla
    plannedCost = 50000
    currency = "RUB"
    note = "storage service"
}
$serviceOrderId = $serviceOrder.id

Write-Host "Complete procurement service order..."
Invoke-Json -Method Post -Url "$ProcurementUrl/service/orders/$serviceOrderId/complete" -Headers $headers -Body @{
    actualCost = 48000
    completedAt = (Get-Date).ToString("o")
    actDocumentId = $null
} | Out-Null

Write-Host "Wait for inventory storage order completion..."
$finalStorageStatus = $null
for ($i = 0; $i -lt 10; $i++) {
    $orders = Invoke-Json -Method Get -Url "$InventoryUrl/equipment/$equipmentId/storage-orders" -Headers $headers
    $match = $orders | Where-Object { $_.id -eq $storageOrderId }
    if ($null -ne $match) {
        $finalStorageStatus = $match.status
    }
    if ($finalStorageStatus -eq "COMPLETED") {
        break
    }
    Start-Sleep -Seconds 3
}

Write-Host "Record valuation..."
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/$equipmentId/valuations" -Headers $headers -Body @{
    valuationAmount = 1200000
    liquidationAmount = 900000
    currency = "RUB"
    valuatedAt = (Get-Date).ToString("o")
    vendorName = "Valuer LLC"
    vendorInn = "7709876543"
    note = "initial valuation"
} | Out-Null

Write-Host "Move equipment to SALE_LISTED..."
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/$equipmentId/status" -Headers $headers -Body @{
    status = "SALE_LISTED"
    reason = "listing"
} | Out-Null

Write-Host "Create sale disposition..."
$disposition = Invoke-Json -Method Post -Url "$InventoryUrl/equipment/$equipmentId/dispositions" -Headers $headers -Body @{
    type = "SALE"
    plannedPrice = 1250000
    currency = "RUB"
    counterpartyName = "Buyer LLC"
    counterpartyInn = "7701112223"
    locationId = $locationId
    note = "auction sale"
}
$dispositionId = $disposition.id

Write-Host "Approve/contract/invoice/pay/complete sale disposition..."
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/dispositions/$dispositionId/approve" -Headers $headers | Out-Null
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/dispositions/$dispositionId/sale/contract" -Headers $headers -Body @{
    saleMethod = "auction"
    lotNumber = "LOT-$locCode"
    contractNumber = "CN-$locCode"
} | Out-Null
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/dispositions/$dispositionId/sale/invoice" -Headers $headers -Body @{
    invoiceNumber = "INV-$locCode"
} | Out-Null
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/dispositions/$dispositionId/sale/paid" -Headers $headers -Body @{
    paidAt = (Get-Date).ToString("o")
} | Out-Null
Invoke-Json -Method Post -Url "$InventoryUrl/equipment/dispositions/$dispositionId/complete" -Headers $headers -Body @{
    actualPrice = 1230000
    performedAt = (Get-Date).ToString("o")
} | Out-Null

Write-Host "Done."
Write-Host "LocationId: $locationId"
Write-Host "EquipmentId: $equipmentId"
Write-Host "StorageOrderId: $storageOrderId (status: $finalStorageStatus)"
Write-Host "ServiceOrderId: $serviceOrderId"
Write-Host "DispositionId: $dispositionId"
