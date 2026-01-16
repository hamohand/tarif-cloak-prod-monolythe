-- Migration pour créer le plan "Essai gratuit" pour le marché DZ
-- Ce plan offre un essai gratuit avec quota limité

INSERT INTO pricing_plan (
    name, 
    description, 
    price_per_month, 
    price_per_request, 
    monthly_quota, 
    trial_period_days,
    features, 
    is_active, 
    display_order, 
    market_version, 
    currency, 
    is_custom, 
    created_at
)
SELECT 
    'Essai gratuit',
    'Plan d''essai gratuit - 100 requêtes pendant 14 jours',
    0.00,
    NULL,
    100,
    14,
    '100 requêtes gratuites, Essai de 14 jours, Accès à toutes les fonctionnalités de base, Support par email',
    true,
    0,  -- display_order = 0 pour qu'il apparaisse en premier
    'DZ',
    'DZD',
    false,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM pricing_plan 
    WHERE name = 'Essai gratuit' 
    AND market_version = 'DZ'
);
