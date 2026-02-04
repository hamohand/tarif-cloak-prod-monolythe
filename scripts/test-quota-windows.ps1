# Script de test manuel pour la Phase 4 : Quotas Basiques (Windows PowerShell)
# Utilisation: .\test-quota-windows.ps1 -AdminToken "YOUR_ADMIN_TOKEN" -UserToken "YOUR_USER_TOKEN" -BaseUrl "https://www.hscode.enclume-numerique.com/api"

param(
    [string]$AdminToken = "YOUR_ADMIN_TOKEN",
    [string]$UserToken = "YOUR_USER_TOKEN",
    [string]$BaseUrl = "https://www.hscode.enclume-numerique.com/api"
)

Write-Host "=== Tests de Quotas Basiques (Phase 4) ===" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl" -ForegroundColor Cyan
Write-Host ""

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Yellow
}

# Test 1 : Créer une organisation
Write-Host "Test 1 : Créer une organisation avec quota" -ForegroundColor Cyan
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$orgBody = @{
    name = "Entreprise Test Quota $timestamp"
} | ConvertTo-Json

try {
    $orgResponse = Invoke-RestMethod -Uri "$BaseUrl/admin/organizations" `
        -Method POST `
        -Headers @{
            "Content-Type" = "application/json"
            "Authorization" = "Bearer $AdminToken"
        } `
        -Body $orgBody
    
    $orgId = $orgResponse.id
    Write-Success "Organisation créée avec ID: $orgId"
} catch {
    Write-Error "Échec de la création de l'organisation"
    Write-Host "Erreur: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 2 : Définir un quota
Write-Host "Test 2 : Définir un quota de 5 requêtes par mois" -ForegroundColor Cyan
$quotaBody = @{
    monthlyQuota = 5
} | ConvertTo-Json

try {
    $quotaResponse = Invoke-RestMethod -Uri "$BaseUrl/admin/organizations/$orgId/quota" `
        -Method PUT `
        -Headers @{
            "Content-Type" = "application/json"
            "Authorization" = "Bearer $AdminToken"
        } `
        -Body $quotaBody
    
    if ($quotaResponse.monthlyQuota -eq 5) {
        Write-Success "Quota défini à 5 requêtes/mois"
    } else {
        Write-Error "Échec de la définition du quota"
        exit 1
    }
} catch {
    Write-Error "Échec de la définition du quota"
    Write-Host "Erreur: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Test 3 : Vérifier le quota
Write-Host "Test 3 : Vérifier que le quota est enregistré" -ForegroundColor Cyan
try {
    $getOrgResponse = Invoke-RestMethod -Uri "$BaseUrl/admin/organizations/$orgId" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $AdminToken"
        }
    
    if ($getOrgResponse.monthlyQuota -eq 5) {
        Write-Success "Quota correctement enregistré"
    } else {
        Write-Error "Le quota n'est pas correctement enregistré"
        exit 1
    }
} catch {
    Write-Error "Échec de la vérification du quota"
    Write-Host "Erreur: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Note : Les tests suivants nécessitent un utilisateur associé à l'organisation
Write-Info "Note: Pour tester les recherches, vous devez d'abord associer un utilisateur à l'organisation"
Write-Info "Utilisez: Invoke-RestMethod -Uri `"$BaseUrl/admin/organizations/$orgId/users`" -Method POST -Headers @{`"Authorization`" = `"Bearer $AdminToken`"} -Body (@{keycloakUserId = `"USER_ID`"} | ConvertTo-Json)"
Write-Host ""

# Test 4 : Vérifier les statistiques
Write-Host "Test 4 : Vérifier les statistiques d'utilisation" -ForegroundColor Cyan
try {
    $statsResponse = Invoke-RestMethod -Uri "$BaseUrl/admin/usage/stats?organizationId=$orgId" `
        -Method GET `
        -Headers @{
            "Authorization" = "Bearer $AdminToken"
        }
    
    Write-Success "Statistiques récupérées avec succès"
    Write-Host "Total requests: $($statsResponse.totalRequests)" -ForegroundColor Gray
} catch {
    Write-Error "Échec de la récupération des statistiques"
    Write-Host "Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 5 : Mettre le quota à null (illimité)
Write-Host "Test 5 : Mettre le quota à null (quota illimité)" -ForegroundColor Cyan
$unlimitedBody = @{
    monthlyQuota = $null
} | ConvertTo-Json -Depth 2

try {
    $unlimitedResponse = Invoke-RestMethod -Uri "$BaseUrl/admin/organizations/$orgId/quota" `
        -Method PUT `
        -Headers @{
            "Content-Type" = "application/json"
            "Authorization" = "Bearer $AdminToken"
        } `
        -Body $unlimitedBody
    
    if ($null -eq $unlimitedResponse.monthlyQuota) {
        Write-Success "Quota mis à illimité (null)"
    } else {
        Write-Error "Échec de la mise à jour du quota à null"
        exit 1
    }
} catch {
    Write-Error "Échec de la mise à jour du quota à null"
    Write-Host "Erreur: $_" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Success "Tous les tests de base sont passés!"
Write-Host ""
Write-Info "Pour tester les recherches avec quota, vous devez:"
Write-Info "1. Associer un utilisateur à l'organisation (ID: $orgId)"
Write-Info "2. Effectuer des recherches avec le token de cet utilisateur"
Write-Info "3. Vérifier que le quota est bien vérifié et que les recherches sont bloquées après dépassement"

