#!/bin/bash

# Script de diagnostic pour l'endpoint /api/alerts/my-alerts/count
# Usage: ./diagnostic-alerts.sh

echo "🔍 Diagnostic de l'endpoint /api/alerts/my-alerts/count"
echo "=================================================="
echo ""

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
BACKEND_CONTAINER="${PROJECT_NAME:-app}-backend"
FRONTEND_DOMAIN="${FRONTEND_DOMAIN:-www.hscode.enclume-numerique.com}"
API_URL="https://${FRONTEND_DOMAIN}/api"

echo "1️⃣ Vérification des conteneurs Docker"
echo "-----------------------------------"
if docker ps | grep -q "${BACKEND_CONTAINER}"; then
    echo -e "${GREEN}✓${NC} Backend container trouvé: ${BACKEND_CONTAINER}"
    BACKEND_STATUS=$(docker ps --filter "name=${BACKEND_CONTAINER}" --format "{{.Status}}")
    echo "   Status: ${BACKEND_STATUS}"
else
    echo -e "${RED}✗${NC} Backend container non trouvé: ${BACKEND_CONTAINER}"
    echo "   Action: Redémarrer le backend avec 'docker-compose -f docker-compose-prod.yml up -d backend'"
    exit 1
fi

echo ""
echo "2️⃣ Vérification de l'endpoint de santé"
echo "-----------------------------------"
HEALTH_RESPONSE=$(curl -s -k -o /dev/null -w "%{http_code}" "${API_URL}/health")
if [ "${HEALTH_RESPONSE}" = "200" ]; then
    echo -e "${GREEN}✓${NC} Endpoint /api/health répond (HTTP ${HEALTH_RESPONSE})"
    HEALTH_BODY=$(curl -s -k "${API_URL}/health")
    echo "   Réponse: ${HEALTH_BODY}"
else
    echo -e "${RED}✗${NC} Endpoint /api/health ne répond pas (HTTP ${HEALTH_RESPONSE})"
    echo "   Problème: Le backend ne répond pas ou le routage Traefik est incorrect"
    
    # Vérifier si la réponse est du HTML
    HEALTH_CONTENT=$(curl -s -k "${API_URL}/health" | head -c 100)
    if echo "${HEALTH_CONTENT}" | grep -q "<!doctype html"; then
        echo -e "${RED}   ⚠️  La réponse est du HTML au lieu de JSON${NC}"
        echo "   Problème: Traefik route vers le frontend au lieu du backend"
    fi
    exit 1
fi

echo ""
echo "3️⃣ Vérification des logs du backend"
echo "-----------------------------------"
echo "Dernières lignes des logs du backend:"
docker logs "${BACKEND_CONTAINER}" --tail 20 2>&1 | grep -i "error\|exception\|started\|alerts" || echo "Aucune ligne pertinente trouvée"

echo ""
echo "4️⃣ Vérification de la table quota_alert"
echo "-----------------------------------"
DB_CONTAINER="${PROJECT_NAME:-app}-app-db"
if docker ps | grep -q "${DB_CONTAINER}"; then
    echo -e "${GREEN}✓${NC} Container de base de données trouvé: ${DB_CONTAINER}"
    
    # Vérifier si la table existe (nécessite les variables d'environnement)
    if docker exec "${DB_CONTAINER}" psql -U "${POSTGRES_USER:-postgres}" -d "${POSTGRES_DB:-postgres}" -c "\dt quota_alert" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} Table quota_alert existe"
    else
        echo -e "${YELLOW}⚠️${NC} Table quota_alert pourrait ne pas exister"
        echo "   Action: Vérifier manuellement avec 'docker exec -it ${DB_CONTAINER} psql -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-postgres}'"
    fi
else
    echo -e "${YELLOW}⚠️${NC} Container de base de données non trouvé: ${DB_CONTAINER}"
fi

echo ""
echo "5️⃣ Test de l'endpoint /api/alerts/my-alerts/count"
echo "-----------------------------------"
echo "Note: Cet endpoint nécessite une authentification"
ALERTS_RESPONSE=$(curl -s -k -o /dev/null -w "%{http_code}" "${API_URL}/alerts/my-alerts/count")
if [ "${ALERTS_RESPONSE}" = "200" ] || [ "${ALERTS_RESPONSE}" = "401" ] || [ "${ALERTS_RESPONSE}" = "403" ]; then
    echo -e "${GREEN}✓${NC} Endpoint /api/alerts/my-alerts/count répond (HTTP ${ALERTS_RESPONSE})"
    if [ "${ALERTS_RESPONSE}" = "401" ] || [ "${ALERTS_RESPONSE}" = "403" ]; then
        echo "   Note: Réponse d'authentification attendue (nécessite un token JWT)"
    fi
else
    echo -e "${RED}✗${NC} Endpoint /api/alerts/my-alerts/count ne répond pas correctement (HTTP ${ALERTS_RESPONSE})"
    
    # Vérifier si la réponse est du HTML
    ALERTS_CONTENT=$(curl -s -k "${API_URL}/alerts/my-alerts/count" | head -c 100)
    if echo "${ALERTS_CONTENT}" | grep -q "<!doctype html"; then
        echo -e "${RED}   ⚠️  La réponse est du HTML au lieu de JSON${NC}"
        echo "   Problème: Traefik route vers le frontend au lieu du backend"
        echo "   Solution: Vérifier la configuration Traefik dans docker-compose-prod.yml"
    fi
fi

echo ""
echo "6️⃣ Vérification de la configuration Traefik"
echo "-----------------------------------"
echo "Vérification des labels Traefik du backend:"
docker inspect "${BACKEND_CONTAINER}" | grep -i "traefik" | head -10 || echo "Aucun label Traefik trouvé"

echo ""
echo "=================================================="
echo "📋 Résumé"
echo "=================================================="
echo ""
echo "Actions recommandées:"
echo "1. Redémarrer le backend: docker-compose -f docker-compose-prod.yml restart backend"
echo "2. Vérifier les logs: docker logs -f ${BACKEND_CONTAINER}"
echo "3. Vérifier la configuration Traefik dans docker-compose-prod.yml"
echo "4. Tester l'endpoint avec un token JWT valide"
echo ""
echo "Pour plus d'informations, voir DIAGNOSTIC_ALERTES_ENDPOINT.md"

