-- Migration pour ajouter les colonnes market_version, currency, is_custom, organization_id à la table pricing_plan

ALTER TABLE pricing_plan 
ADD COLUMN IF NOT EXISTS market_version VARCHAR(10) DEFAULT 'DEFAULT',
ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'EUR',
ADD COLUMN IF NOT EXISTS is_custom BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS organization_id BIGINT;

-- Mettre à jour les plans existants pour avoir market_version = 'DEFAULT'
UPDATE pricing_plan SET market_version = 'DEFAULT' WHERE market_version IS NULL;
UPDATE pricing_plan SET currency = 'EUR' WHERE currency IS NULL;
UPDATE pricing_plan SET is_custom = false WHERE is_custom IS NULL;

-- Index pour améliorer les performances des recherches par version de marché
CREATE INDEX IF NOT EXISTS idx_pricing_plan_market_version ON pricing_plan(market_version);
CREATE INDEX IF NOT EXISTS idx_pricing_plan_organization_id ON pricing_plan(organization_id);

