#!/bin/bash

# Script de test manuel pour la Phase 4 : Quotas Basiques
# Utilisation: ./test-quota.sh [ADMIN_TOKEN] [USER_TOKEN] [BASE_URL]

set -e

ADMIN_TOKEN="${1:-YOUR_ADMIN_TOKEN}"
USER_TOKEN="${2:-YOUR_USER_TOKEN}"
BASE_URL="${3:-https://www.hscode.enclume-numerique.com/api}"

echo "=== Tests de Quotas Basiques (Phase 4) ==="
echo "Base URL: $BASE_URL"
echo ""

# Couleurs pour les messages
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction pour afficher les résultats
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Test 1 : Créer une organisation
echo "Test 1 : Créer une organisation avec quota"
ORG_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/organizations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Entreprise Test Quota '$(date +%s)'"
  }')

ORG_ID=$(echo $ORG_RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$ORG_ID" ]; then
    print_error "Échec de la création de l'organisation"
    echo "Réponse: $ORG_RESPONSE"
    exit 1
fi

print_success "Organisation créée avec ID: $ORG_ID"
echo ""

# Test 2 : Définir un quota
echo "Test 2 : Définir un quota de 5 requêtes par mois"
QUOTA_RESPONSE=$(curl -s -X PUT "$BASE_URL/admin/organizations/$ORG_ID/quota" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "monthlyQuota": 5
  }')

if echo "$QUOTA_RESPONSE" | grep -q '"monthlyQuota":5'; then
    print_success "Quota défini à 5 requêtes/mois"
else
    print_error "Échec de la définition du quota"
    echo "Réponse: $QUOTA_RESPONSE"
    exit 1
fi
echo ""

# Test 3 : Vérifier le quota
echo "Test 3 : Vérifier que le quota est enregistré"
GET_ORG_RESPONSE=$(curl -s -X GET "$BASE_URL/admin/organizations/$ORG_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$GET_ORG_RESPONSE" | grep -q '"monthlyQuota":5'; then
    print_success "Quota correctement enregistré"
else
    print_error "Le quota n'est pas correctement enregistré"
    echo "Réponse: $GET_ORG_RESPONSE"
    exit 1
fi
echo ""

# Note : Les tests suivants nécessitent un utilisateur associé à l'organisation
print_info "Note: Pour tester les recherches, vous devez d'abord associer un utilisateur à l'organisation"
print_info "Utilisez: curl -X POST \"$BASE_URL/admin/organizations/$ORG_ID/users\" -H \"Authorization: Bearer $ADMIN_TOKEN\" -d '{\"keycloakUserId\": \"USER_ID\"}'"
echo ""

# Test 4 : Vérifier les statistiques
echo "Test 4 : Vérifier les statistiques d'utilisation"
STATS_RESPONSE=$(curl -s -X GET "$BASE_URL/admin/usage/stats?organizationId=$ORG_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

if echo "$STATS_RESPONSE" | grep -q "totalRequests"; then
    print_success "Statistiques récupérées avec succès"
    echo "Statistiques: $STATS_RESPONSE" | head -c 200
    echo "..."
else
    print_error "Échec de la récupération des statistiques"
    echo "Réponse: $STATS_RESPONSE"
fi
echo ""

# Test 5 : Mettre le quota à null (illimité)
echo "Test 5 : Mettre le quota à null (quota illimité)"
UNLIMITED_RESPONSE=$(curl -s -X PUT "$BASE_URL/admin/organizations/$ORG_ID/quota" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "monthlyQuota": null
  }')

if echo "$UNLIMITED_RESPONSE" | grep -q '"monthlyQuota":null'; then
    print_success "Quota mis à illimité (null)"
else
    print_error "Échec de la mise à jour du quota à null"
    echo "Réponse: $UNLIMITED_RESPONSE"
    exit 1
fi
echo ""

print_success "Tous les tests de base sont passés!"
echo ""
print_info "Pour tester les recherches avec quota, vous devez:"
print_info "1. Associer un utilisateur à l'organisation (ID: $ORG_ID)"
print_info "2. Effectuer des recherches avec le token de cet utilisateur"
print_info "3. Vérifier que le quota est bien vérifié et que les recherches sont bloquées après dépassement"

