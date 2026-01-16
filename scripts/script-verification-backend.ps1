# Script de vérification du backend (PowerShell)
# Usage: .\script-verification-backend.ps1

Write-Host "🔍 Vérification du Backend" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan
Write-Host ""

$BACKEND_CONTAINER = "hscode-backend"
$API_URL = "https://www.hscode.enclume-numerique.com/api"

# Fonction pour afficher un message de succès
function Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

# Fonction pour afficher un message d'erreur
function Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# Fonction pour afficher un message d'avertissement
function Warning {
    param([string]$Message)
    Write-Host "⚠️ $Message" -ForegroundColor Yellow
}

# Fonction pour afficher une information
function Info {
    param([string]$Message)
    Write-Host "ℹ️ $Message" -ForegroundColor Blue
}

Write-Host "1️⃣ Vérification des Variables d'Environnement" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan

if (Test-Path ".env") {
    Success "Fichier .env trouvé"
    
    $envContent = Get-Content ".env"
    if ($envContent -match "POSTGRES_USER") {
        $postgresUser = ($envContent | Select-String "POSTGRES_USER").ToString().Split("=")[1]
        Info "POSTGRES_USER: $postgresUser"
    } else {
        Error "POSTGRES_USER non trouvé dans .env"
    }
    
    if ($envContent -match "POSTGRES_PASSWORD") {
        Success "POSTGRES_PASSWORD trouvé dans .env"
    } else {
        Error "POSTGRES_PASSWORD non trouvé dans .env"
    }
    
    if ($envContent -match "POSTGRES_DB") {
        $postgresDb = ($envContent | Select-String "POSTGRES_DB").ToString().Split("=")[1]
        Info "POSTGRES_DB: $postgresDb"
    } else {
        Error "POSTGRES_DB non trouvé dans .env"
    }
} else {
    Error "Fichier .env non trouvé"
}

Write-Host ""
Write-Host "2️⃣ Vérification de la Base de Données" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan

$dbContainer = docker ps --filter "name=app-db" --format "{{.Names}}" | Select-Object -First 1

if ([string]::IsNullOrEmpty($dbContainer)) {
    Error "Conteneur de base de données non trouvé"
    Info "Tentative de démarrage..."
    docker-compose -f docker-compose-prod.yml up -d app-db
    Start-Sleep -Seconds 5
    $dbContainer = docker ps --filter "name=app-db" --format "{{.Names}}" | Select-Object -First 1
}

if (-not [string]::IsNullOrEmpty($dbContainer)) {
    Success "Conteneur de base de données trouvé: $dbContainer"
    
    $dbStatus = docker ps --filter "name=$dbContainer" --format "{{.Status}}"
    Info "Status: $dbStatus"
} else {
    Error "Impossible de trouver ou démarrer le conteneur de base de données"
}

Write-Host ""
Write-Host "3️⃣ Vérification du Backend" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan

$backendRunning = docker ps --format "{{.Names}}" | Select-String -Pattern $BACKEND_CONTAINER

if ($backendRunning) {
    Success "Backend en cours d'exécution"
    $backendStatus = docker ps --filter "name=$BACKEND_CONTAINER" --format "{{.Status}}"
    Info "Status: $backendStatus"
} else {
    Error "Backend non en cours d'exécution"
    
    $backendExists = docker ps -a --format "{{.Names}}" | Select-String -Pattern $BACKEND_CONTAINER
    
    if ($backendExists) {
        Warning "Backend trouvé mais arrêté"
        Info "Tentative de démarrage..."
        docker-compose -f docker-compose-prod.yml up -d backend
        Start-Sleep -Seconds 10
    } else {
        Error "Conteneur backend non trouvé"
        Info "Tentative de création..."
        docker-compose -f docker-compose-prod.yml up -d --build backend
        Start-Sleep -Seconds 10
    }
}

Write-Host ""
Write-Host "4️⃣ Vérification des Logs du Backend" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan

$backendRunning = docker ps --format "{{.Names}}" | Select-String -Pattern $BACKEND_CONTAINER

if ($backendRunning) {
    $logs = docker logs $BACKEND_CONTAINER --tail 50 2>&1
    
    if ($logs -match "FATAL|Exception|Error") {
        Error "Erreurs trouvées dans les logs du backend"
        Write-Host ""
        Info "Dernières erreurs:"
        $logs | Select-String -Pattern "FATAL|Exception|Error" | Select-Object -Last 5
    } else {
        Success "Aucune erreur dans les logs du backend"
    }
    
    if ($logs -match "Started BackendApplication|Backend Application Started") {
        Success "Backend démarré avec succès"
    } else {
        Warning "Backend peut ne pas avoir démarré correctement"
    }
} else {
    Error "Impossible de vérifier les logs (backend non en cours d'exécution)"
}

Write-Host ""
Write-Host "5️⃣ Test de l'Endpoint de Santé" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan

try {
    $response = Invoke-WebRequest -Uri "$API_URL/health" -Method GET -SkipCertificateCheck -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Success "Endpoint de santé répond (HTTP $($response.StatusCode))"
        Info "Réponse: $($response.Content)"
    } else {
        Error "Endpoint de santé retourne HTTP $($response.StatusCode)"
    }
} catch {
    Error "Impossible de se connecter à l'endpoint de santé"
    Info "Vérifiez que le backend est en cours d'exécution et que Traefik est configuré correctement"
}

Write-Host ""
Write-Host "6️⃣ Vérification du Réseau Docker" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan

$webproxyExists = docker network ls --format "{{.Name}}" | Select-String -Pattern "webproxy"

if ($webproxyExists) {
    Success "Réseau webproxy existe"
} else {
    Error "Réseau webproxy n'existe pas"
    Info "Création du réseau..."
    docker network create webproxy
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "📋 Résumé" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Résumé
$backendRunning = docker ps --format "{{.Names}}" | Select-String -Pattern $BACKEND_CONTAINER
if ($backendRunning) {
    Success "Backend: En cours d'exécution"
} else {
    Error "Backend: Non en cours d'exécution"
}

if (-not [string]::IsNullOrEmpty($dbContainer)) {
    $dbRunning = docker ps --format "{{.Names}}" | Select-String -Pattern $dbContainer
    if ($dbRunning) {
        Success "Base de données: En cours d'exécution"
    } else {
        Error "Base de données: Non en cours d'exécution"
    }
} else {
    Error "Base de données: Non trouvée"
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "🔧 Actions Recommandées" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

if (-not $backendRunning) {
    Write-Host "1. Redémarrer le backend:"
    Write-Host "   docker-compose -f docker-compose-prod.yml up -d backend"
    Write-Host ""
}

Write-Host "2. Vérifier les logs du backend:"
Write-Host "   docker logs $BACKEND_CONTAINER --tail 100"
Write-Host ""

Write-Host "3. Pour suivre les logs en temps réel:"
Write-Host "   docker logs -f $BACKEND_CONTAINER"
Write-Host ""

Write-Host "4. Pour tester l'endpoint de santé:"
Write-Host "   curl -k $API_URL/health"
Write-Host ""

