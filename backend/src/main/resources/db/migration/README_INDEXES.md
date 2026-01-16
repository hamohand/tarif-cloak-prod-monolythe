# Index de Performance - Base de Données

## 📋 Description

Ce script ajoute des index sur les tables fréquemment interrogées pour améliorer les performances des requêtes.

## 🚀 Utilisation

### Option 1 : Exécution Manuelle (Recommandé pour le moment)

```bash
# Se connecter à la base de données PostgreSQL
docker exec -it <container-postgres> psql -U <user> -d <database>

# Exécuter le script
\i /path/to/add_performance_indexes.sql
```

Ou en une commande :

```bash
docker exec -i <container-postgres> psql -U <user> -d <database> < backend/src/main/resources/db/migration/add_performance_indexes.sql
```

### Option 2 : Via Flyway (Si configuré)

Si vous utilisez Flyway, placez le script dans le répertoire approprié et il sera exécuté automatiquement.

## 📊 Index Créés

### Table `usage_log`

1. **idx_usage_log_organization_id** : Pour les requêtes filtrées par organisation
2. **idx_usage_log_timestamp** : Pour les requêtes filtrées par date
3. **idx_usage_log_org_timestamp** : Pour les requêtes combinées (org + date) - **Le plus important**
4. **idx_usage_log_user_id** : Pour les requêtes filtrées par utilisateur
5. **idx_usage_log_user_timestamp** : Pour les statistiques utilisateur

### Table `quota_alert`

1. **idx_quota_alert_organization_id** : Pour les requêtes d'alertes par organisation
2. **idx_quota_alert_is_read** : Pour les requêtes d'alertes non lues
3. **idx_quota_alert_created_at** : Pour le tri par date
4. **idx_quota_alert_org_read** : Pour les alertes non lues d'une organisation

### Table `organization_user`

1. **idx_organization_user_org_id** : Pour les jointures par organisation
2. **idx_organization_user_keycloak_id** : Pour les recherches par utilisateur

### Table `organization`

1. **idx_organization_email** : Index partiel pour les recherches par email (uniquement si email non null)

## ⚠️ Notes Importantes

1. **Impact sur les écritures** : Les index ralentissent légèrement les opérations d'écriture (INSERT, UPDATE, DELETE), mais améliorent considérablement les opérations de lecture (SELECT).

2. **Espace disque** : Les index occupent de l'espace disque supplémentaire (généralement 10-20% de la taille de la table).

3. **Maintenance** : PostgreSQL maintient automatiquement les index. Cependant, il est recommandé d'exécuter `ANALYZE` périodiquement pour mettre à jour les statistiques.

4. **Production** : En production, exécutez ce script pendant une période de faible charge si possible.

## 🔍 Vérification

Pour vérifier que les index ont été créés :

```sql
-- Lister tous les index sur usage_log
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'usage_log';

-- Vérifier l'utilisation des index
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE tablename IN ('usage_log', 'quota_alert', 'organization_user', 'organization')
ORDER BY idx_scan DESC;
```

## 📈 Amélioration Attendue

- **Requêtes de statistiques** : 5-10x plus rapides
- **Vérifications de quota** : 3-5x plus rapides
- **Recherches d'alertes** : 2-3x plus rapides
- **Jointures** : 2-4x plus rapides

## 🔄 Rollback

Si vous devez supprimer les index :

```sql
DROP INDEX IF EXISTS idx_usage_log_organization_id;
DROP INDEX IF EXISTS idx_usage_log_timestamp;
DROP INDEX IF EXISTS idx_usage_log_org_timestamp;
DROP INDEX IF EXISTS idx_usage_log_user_id;
DROP INDEX IF EXISTS idx_usage_log_user_timestamp;
DROP INDEX IF EXISTS idx_quota_alert_organization_id;
DROP INDEX IF EXISTS idx_quota_alert_is_read;
DROP INDEX IF EXISTS idx_quota_alert_created_at;
DROP INDEX IF EXISTS idx_quota_alert_org_read;
DROP INDEX IF EXISTS idx_organization_user_org_id;
DROP INDEX IF EXISTS idx_organization_user_keycloak_id;
DROP INDEX IF EXISTS idx_organization_email;
```

