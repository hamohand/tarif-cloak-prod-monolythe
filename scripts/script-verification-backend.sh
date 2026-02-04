#!/bin/bash

# Script de vérification du backend
# Usage: ./script-verification-backend.sh

echo "🔍 Vérification du Backend"
echo "========================"
echo ""

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BACKEND_CONTAINER="hscode-backend"
DB_CONTAINER=""
API_URL="https://www.hscode.enclume-numerique.com/api"

# Fonction pour afficher un message de succès
success() {
    echo -e "${GREEN}✓${NC} $1"
}

# Fonction pour afficher un message d'erreur
error() {
    echo -e "${RED}✗${NC} $1"
}

# Fonction pour afficher un message d'avertissement
warning() {
    echo -e "${YELLOW}⚠️${NC} $1"
}

# Fonction pour afficher une information
info() {
    echo -e "${BLUE}ℹ️${NC} $1"
}

echo "1️⃣ Vérification des Variables d'Environnement"
echo "-----------------------------------"

if [ -f ".env" ]; then
    success "Fichier .env trouvé"
    
    if grep -q "POSTGRES_USER" .env; then
        POSTGRES_USER=$(grep "POSTGRES_USER" .env | cut -d '=' -f2)
        info "POSTGRES_USER: $POSTGRES_USER"
    else
        error "POSTGRES_USER non trouvé dans .env"
    fi
    
    if grep -q "POSTGRES_PASSWORD" .env; then
        success "POSTGRES_PASSWORD trouvé dans .env"
    else
        error "POSTGRES_PASSWORD non trouvé dans .env"
    fi
    
    if grep -q "POSTGRES_DB" .env; then
        POSTGRES_DB=$(grep "POSTGRES_DB" .env | cut -d '=' -f2)
        info "POSTGRES_DB: $POSTGRES_DB"
    else
        error "POSTGRES_DB non trouvé dans .env"
    fi
else
    error "Fichier .env non trouvé"
fi

echo ""
echo "2️⃣ Vérification de la Base de Données"
echo "-----------------------------------"

DB_CONTAINER=$(docker ps --filter "name=app-db" --format "{{.Names}}" | head -1)

if [ -z "$DB_CONTAINER" ]; then
    error "Conteneur de base de données non trouvé"
    info "Tentative de démarrage..."
    docker-compose -f docker-compose-prod.yml up -d app-db
    sleep 5
    DB_CONTAINER=$(docker ps --filter "name=app-db" --format "{{.Names}}" | head -1)
fi

if [ -n "$DB_CONTAINER" ]; then
    success "Conteneur de base de données trouvé: $DB_CONTAINER"
    
    DB_STATUS=$(docker ps --filter "name=$DB_CONTAINER" --format "{{.Status}}")
    info "Status: $DB_STATUS"
    
    # Vérifier les logs de la base de données
    if docker logs "$DB_CONTAINER" --tail 10 2>&1 | grep -qi "error"; then
        warning "Erreurs trouvées dans les logs de la base de données"
    else
        success "Aucune erreur dans les logs de la base de données"
    fi
else
    error "Impossible de trouver ou démarrer le conteneur de base de données"
fi

echo ""
echo "3️⃣ Vérification du Backend"
echo "-----------------------------------"

if docker ps | grep -q "$BACKEND_CONTAINER"; then
    success "Backend en cours d'exécution"
    BACKEND_STATUS=$(docker ps --filter "name=$BACKEND_CONTAINER" --format "{{.Status}}")
    info "Status: $BACKEND_STATUS"
else
    error "Backend non en cours d'exécution"
    
    if docker ps -a | grep -q "$BACKEND_CONTAINER"; then
        warning "Backend trouvé mais arrêté"
        info "Tentative de démarrage..."
        docker-compose -f docker-compose-prod.yml up -d backend
        sleep 10
    else
        error "Conteneur backend non trouvé"
        info "Tentative de création..."
        docker-compose -f docker-compose-prod.yml up -d --build backend
        sleep 10
    fi
fi

echo ""
echo "4️⃣ Vérification des Logs du Backend"
echo "-----------------------------------"

if docker ps | grep -q "$BACKEND_CONTAINER"; then
    # Vérifier les erreurs dans les logs
    if docker logs "$BACKEND_CONTAINER" --tail 50 2>&1 | grep -qi "FATAL\|Exception\|Error"; then
        error "Erreurs trouvées dans les logs du backend"
        echo ""
        info "Dernières erreurs:"
        docker logs "$BACKEND_CONTAINER" --tail 50 2>&1 | grep -i "FATAL\|Exception\|Error" | tail -5
    else
        success "Aucune erreur dans les logs du backend"
    fi
    
    # Vérifier que le backend a démarré
    if docker logs "$BACKEND_CONTAINER" --tail 50 2>&1 | grep -qi "Started BackendApplication\|Backend Application Started"; then
        success "Backend démarré avec succès"
    else
        warning "Backend peut ne pas avoir démarré correctement"
    fi
else
    error "Impossible de vérifier les logs (backend non en cours d'exécution)"
fi

echo ""
echo "5️⃣ Test de l'Endpoint de Santé"
echo "-----------------------------------"

HEALTH_RESPONSE=$(curl -s -k -o /dev/null -w "%{http_code}" "$API_URL/health" 2>/dev/null || echo "000")

if [ "$HEALTH_RESPONSE" = "200" ]; then
    success "Endpoint de santé répond (HTTP $HEALTH_RESPONSE)"
    HEALTH_BODY=$(curl -s -k "$API_URL/health" 2>/dev/null)
    info "Réponse: $HEALTH_BODY"
else
    if [ "$HEALTH_RESPONSE" = "000" ]; then
        error "Impossible de se connecter à l'endpoint de santé"
        info "Vérifiez que le backend est en cours d'exécution et que Traefik est configuré correctement"
    else
        error "Endpoint de santé retourne HTTP $HEALTH_RESPONSE"
    fi
fi

echo ""
echo "6️⃣ Vérification du Réseau Docker"
echo "-----------------------------------"

if docker network ls | grep -q "webproxy"; then
    success "Réseau webproxy existe"
else
    error "Réseau webproxy n'existe pas"
    info "Création du réseau..."
    docker network create webproxy
fi

if docker network inspect webproxy 2>/dev/null | grep -q "$BACKEND_CONTAINER"; then
    success "Backend connecté au réseau webproxy"
else
    warning "Backend peut ne pas être connecté au réseau webproxy"
fi

echo ""
echo "=================================================="
echo "📋 Résumé"
echo "=================================================="
echo ""

# Résumé
if docker ps | grep -q "$BACKEND_CONTAINER"; then
    success "Backend: En cours d'exécution"
else
    error "Backend: Non en cours d'exécution"
fi

if [ -n "$DB_CONTAINER" ] && docker ps | grep -q "$DB_CONTAINER"; then
    success "Base de données: En cours d'exécution"
else
    error "Base de données: Non en cours d'exécution"
fi

if [ "$HEALTH_RESPONSE" = "200" ]; then
    success "Endpoint de santé: Accessible"
else
    error "Endpoint de santé: Non accessible"
fi

echo ""
echo "=================================================="
echo "🔧 Actions Recommandées"
echo "=================================================="
echo ""

if ! docker ps | grep -q "$BACKEND_CONTAINER"; then
    echo "1. Redémarrer le backend:"
    echo "   docker-compose -f docker-compose-prod.yml up -d backend"
    echo ""
fi

if [ "$HEALTH_RESPONSE" != "200" ]; then
    echo "2. Vérifier les logs du backend:"
    echo "   docker logs $BACKEND_CONTAINER --tail 100"
    echo ""
fi

if [ -z "$DB_CONTAINER" ] || ! docker ps | grep -q "$DB_CONTAINER"; then
    echo "3. Vérifier la base de données:"
    echo "   docker-compose -f docker-compose-prod.yml ps app-db"
    echo ""
fi

echo "4. Pour suivre les logs en temps réel:"
echo "   docker logs -f $BACKEND_CONTAINER"
echo ""

echo "5. Pour tester l'endpoint de santé:"
echo "   curl -k $API_URL/health"
echo ""

