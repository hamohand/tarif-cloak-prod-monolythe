-- Migration pour créer le plan tarifaire DZ "Forfait" - 300 requêtes pour 10000 DA/mois
-- Cette migration est idempotente (peut être exécutée plusieurs fois sans erreur)

INSERT INTO pricing_plan (name, description, price_per_month, price_per_request, monthly_quota, features, is_active, display_order, market_version, currency, is_custom, created_at)
SELECT 
    'Forfait',
    'Forfait mensuel - 300 requêtes pour 10000 DA/mois',
    10000.00,
    NULL,  -- Pas de prix par requête car c'est un forfait mensuel
    300,   -- Quota mensuel de 300 requêtes
    '300 requêtes par mois, Facturation mensuelle, Support par email, Renouvellement automatique',
    true,
    2,     -- Display order 2 (après le plan Pay-per-Request qui est à 1)
    'DZ',
    'DZD',
    false,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_plan 
    WHERE name = 'Forfait' 
    AND market_version = 'DZ'
);

