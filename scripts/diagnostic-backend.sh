#!/bin/bash

# Script de diagnostic pour l'erreur 502
# Usage: ./diagnostic-backend.sh

echo "=========================================="
echo "DIAGNOSTIC BACKEND - Erreur 502"
echo "=========================================="
echo ""

echo "1. Vérification des conteneurs Docker..."
docker compose ps
echo ""

echo "2. Vérification des logs du backend (dernières 50 lignes)..."
docker compose logs backend --tail=50
echo ""

echo "3. Vérification de la santé du backend..."
docker compose exec -T backend curl -s http://localhost:8081/actuator/health || echo "❌ Le backend ne répond pas sur le port 8081"
echo ""

echo "4. Vérification de l'existence de la table usage_log..."
docker compose exec -T app-db psql -U muhend -d app-db -c "\dt usage_log" || echo "❌ La table usage_log n'existe pas"
echo ""

echo "5. Vérification de la configuration JPA (ddl-auto)..."
echo "   Vérifiez manuellement dans application.yml que ddl-auto=update est activé"
echo ""

echo "6. Test de connexion à la base de données depuis le backend..."
docker compose exec -T backend sh -c "echo 'SELECT 1;' | psql -h app-db -U muhend -d app-db" || echo "❌ Le backend ne peut pas se connecter à la base de données"
echo ""

echo "=========================================="
echo "ACTIONS RECOMMANDÉES"
echo "=========================================="
echo "1. Si le backend ne démarre pas :"
echo "   docker compose restart backend"
echo ""
echo "2. Si la table n'existe pas :"
echo "   - Vérifier que ddl-auto=update est activé dans application.yml"
echo "   - Redémarrer le backend : docker compose restart backend"
echo ""
echo "3. Vérifier les logs en temps réel :"
echo "   docker compose logs -f backend"
echo ""

