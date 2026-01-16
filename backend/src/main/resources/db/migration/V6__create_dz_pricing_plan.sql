-- Migration pour créer le plan tarifaire DZ "Pay-per-Request" à 50 DA la requête

INSERT INTO pricing_plan (name, description, price_per_request, monthly_quota, features, is_active, display_order, market_version, currency, is_custom, created_at)
VALUES (
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
);

