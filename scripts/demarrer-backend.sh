#!/bin/bash

# Script pour démarrer le backend et diagnostiquer les problèmes
# Usage: ./demarrer-backend.sh

echo "🚀 Démarrage du backend..."
echo "========================"
echo ""

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Vérifier que Docker Compose est disponible
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗${NC} docker-compose n'est pas installé"
    exit 1
fi

# Vérifier les dépendances
echo "1️⃣ Vérification des dépendances..."
echo "-----------------------------------"

# Vérifier la base de données
if docker ps | grep -q "app-db\|hscode.*db"; then
    echo -e "${GREEN}✓${NC} Base de données en cours d'exécution"
else
    echo -e "${YELLOW}⚠️${NC} Base de données non trouvée. Démarrage..."
    docker-compose -f docker-compose-prod.yml up -d app-db
    sleep 5
fi

# Vérifier Keycloak
if docker ps | grep -q "keycloak\|hscode.*keycloak"; then
    echo -e "${GREEN}✓${NC} Keycloak en cours d'exécution"
else
    echo -e "${YELLOW}⚠️${NC} Keycloak non trouvé. Démarrage..."
    docker-compose -f docker-compose-prod.yml up -d keycloak
    sleep 10
fi

# Vérifier le réseau webproxy
if docker network ls | grep -q "webproxy"; then
    echo -e "${GREEN}✓${NC} Réseau webproxy existe"
else
    echo -e "${RED}✗${NC} Réseau webproxy n'existe pas. Création..."
    docker network create webproxy
fi

echo ""
echo "2️⃣ Vérification des logs du backend (avant redémarrage)..."
echo "-----------------------------------"
docker logs hscode-backend --tail 50 2>&1 | tail -20

echo ""
echo "3️⃣ Démarrage du backend..."
echo "-----------------------------------"
docker-compose -f docker-compose-prod.yml up -d --build backend

echo ""
echo "4️⃣ Attente du démarrage (10 secondes)..."
echo "-----------------------------------"
sleep 10

echo ""
echo "5️⃣ Vérification de l'état du backend..."
echo "-----------------------------------"
if docker ps | grep -q "hscode-backend\|.*backend"; then
    echo -e "${GREEN}✓${NC} Backend en cours d'exécution"
    BACKEND_STATUS=$(docker ps --filter "name=backend" --format "{{.Status}}")
    echo "   Status: ${BACKEND_STATUS}"
else
    echo -e "${RED}✗${NC} Backend non démarré"
    echo ""
    echo "📋 Derniers logs du backend:"
    docker logs hscode-backend --tail 50 2>&1 | tail -30
    exit 1
fi

echo ""
echo "6️⃣ Vérification des logs du backend (après démarrage)..."
echo "-----------------------------------"
docker logs hscode-backend --tail 30 2>&1 | grep -i "started\|error\|exception\|failed" || echo "Aucune ligne pertinente trouvée"

echo ""
echo "7️⃣ Test de l'endpoint de santé..."
echo "-----------------------------------"
HEALTH_RESPONSE=$(curl -s -k -o /dev/null -w "%{http_code}" https://www.hscode.enclume-numerique.com/api/health 2>/dev/null || echo "000")
if [ "${HEALTH_RESPONSE}" = "200" ]; then
    echo -e "${GREEN}✓${NC} Endpoint de santé répond (HTTP ${HEALTH_RESPONSE})"
    HEALTH_BODY=$(curl -s -k https://www.hscode.enclume-numerique.com/api/health 2>/dev/null)
    echo "   Réponse: ${HEALTH_BODY}"
else
    echo -e "${YELLOW}⚠️${NC} Endpoint de santé ne répond pas (HTTP ${HEALTH_RESPONSE})"
    echo "   Cela peut être normal si le backend vient de démarrer. Attendez quelques secondes."
fi

echo ""
echo "=================================================="
echo "✅ Démarrage terminé"
echo "=================================================="
echo ""
echo "Pour suivre les logs en temps réel:"
echo "  docker logs -f hscode-backend"
echo ""
echo "Pour vérifier l'état:"
echo "  docker ps | grep backend"
echo ""
echo "Pour tester l'endpoint de santé:"
echo "  curl -k https://www.hscode.enclume-numerique.com/api/health"

