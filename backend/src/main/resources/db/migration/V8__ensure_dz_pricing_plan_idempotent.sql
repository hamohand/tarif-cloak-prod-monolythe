-- Migration pour s'assurer que le plan DZ existe (idempotente)
-- Cette migration peut être exécutée plusieurs fois sans erreur

INSERT INTO pricing_plan (name, description, price_per_request, monthly_quota, features, is_active, display_order, market_version, currency, is_custom, created_at)
SELECT 
    'Pay-per-Request',
    'Plan facturé à la requête - 50 DA par requête',
    50.00,
    NULL,
    'Facturation à la requête, Pas de quota mensuel, Support par email',
    true,
    1,
    'DZ',
    'DZD',
    false,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_plan 
    WHERE name = 'Pay-per-Request' 
    AND market_version = 'DZ'
);

