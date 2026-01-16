-- Migration pour créer la table market_profile
-- Cette table contient les informations sur les profils de marché (pays/régions)

CREATE TABLE IF NOT EXISTS market_profile (
    id BIGSERIAL PRIMARY KEY,
    market_version VARCHAR(10) NOT NULL UNIQUE,
    country_code_iso_alpha2 VARCHAR(2) NOT NULL UNIQUE,
    country_code_iso_alpha3 VARCHAR(3),
    country_name VARCHAR(100) NOT NULL,
    country_name_native VARCHAR(100),
    phone_prefix VARCHAR(10) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    currency_symbol VARCHAR(10),
    timezone VARCHAR(50),
    locale VARCHAR(10),
    language_code VARCHAR(5),
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT uk_market_profile_market_version UNIQUE (market_version),
    CONSTRAINT uk_market_profile_country_code UNIQUE (country_code_iso_alpha2)
);

-- Index pour améliorer les performances de recherche
CREATE INDEX IF NOT EXISTS idx_market_profile_market_version ON market_profile(market_version);
CREATE INDEX IF NOT EXISTS idx_market_profile_country_code ON market_profile(country_code_iso_alpha2);
CREATE INDEX IF NOT EXISTS idx_market_profile_is_active ON market_profile(is_active);
CREATE INDEX IF NOT EXISTS idx_market_profile_display_order ON market_profile(display_order);

