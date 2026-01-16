-- Migration pour insérer les profils de marché par défaut (DEFAULT et DZ)
-- Cette migration est idempotente (peut être exécutée plusieurs fois sans erreur)

-- Profil DEFAULT (Europe/France)
INSERT INTO market_profile (
    market_version,
    country_code_iso_alpha2,
    country_code_iso_alpha3,
    country_name,
    country_name_native,
    phone_prefix,
    currency_code,
    currency_symbol,
    timezone,
    locale,
    language_code,
    is_active,
    display_order,
    description,
    created_at
)
SELECT 
    'DEFAULT',
    'FR',
    'FRA',
    'France',
    'France',
    '+33',
    'EUR',
    '€',
    'Europe/Paris',
    'fr_FR',
    'fr',
    true,
    1,
    'Profil par défaut pour le marché européen (France)',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM market_profile WHERE market_version = 'DEFAULT'
);

-- Profil DZ (Algérie)
INSERT INTO market_profile (
    market_version,
    country_code_iso_alpha2,
    country_code_iso_alpha3,
    country_name,
    country_name_native,
    phone_prefix,
    currency_code,
    currency_symbol,
    timezone,
    locale,
    language_code,
    is_active,
    display_order,
    description,
    created_at
)
SELECT 
    'DZ',
    'DZ',
    'DZA',
    'Algérie',
    'الجزائر',
    '+213',
    'DZD',
    'DA',
    'Africa/Algiers',
    'ar_DZ',
    'ar',
    true,
    2,
    'Profil pour le marché algérien',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM market_profile WHERE market_version = 'DZ'
);

