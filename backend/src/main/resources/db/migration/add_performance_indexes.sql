-- Script SQL pour ajouter des index de performance
-- Ces index améliorent les performances des requêtes fréquentes sur usage_log

-- Index sur organization_id pour les requêtes filtrées par organisation
CREATE INDEX IF NOT EXISTS idx_usage_log_organization_id 
ON usage_log(organization_id);

-- Index sur timestamp pour les requêtes filtrées par date
CREATE INDEX IF NOT EXISTS idx_usage_log_timestamp 
ON usage_log(timestamp);

-- Index composite sur organization_id et timestamp pour les requêtes combinées
-- (très utilisé pour les statistiques et vérifications de quota)
CREATE INDEX IF NOT EXISTS idx_usage_log_org_timestamp 
ON usage_log(organization_id, timestamp);

-- Index sur keycloak_user_id pour les requêtes filtrées par utilisateur
CREATE INDEX IF NOT EXISTS idx_usage_log_user_id 
ON usage_log(keycloak_user_id);

-- Index composite sur keycloak_user_id et timestamp pour les statistiques utilisateur
CREATE INDEX IF NOT EXISTS idx_usage_log_user_timestamp 
ON usage_log(keycloak_user_id, timestamp);

-- Index sur quota_alert pour améliorer les requêtes d'alertes
CREATE INDEX IF NOT EXISTS idx_quota_alert_organization_id 
ON quota_alert(organization_id);

CREATE INDEX IF NOT EXISTS idx_quota_alert_is_read 
ON quota_alert(is_read);

CREATE INDEX IF NOT EXISTS idx_quota_alert_created_at 
ON quota_alert(created_at);

-- Index composite pour les alertes non lues d'une organisation
CREATE INDEX IF NOT EXISTS idx_quota_alert_org_read 
ON quota_alert(organization_id, is_read);

-- Index sur organization_user pour améliorer les jointures
CREATE INDEX IF NOT EXISTS idx_organization_user_org_id 
ON organization_user(organization_id);

CREATE INDEX IF NOT EXISTS idx_organization_user_keycloak_id 
ON organization_user(keycloak_user_id);

-- Index sur organization pour améliorer les recherches
CREATE INDEX IF NOT EXISTS idx_organization_email 
ON organization(email) 
WHERE email IS NOT NULL;

-- Statistiques sur les tables pour aider le planificateur de requêtes
ANALYZE usage_log;
ANALYZE quota_alert;
ANALYZE organization_user;
ANALYZE organization;

