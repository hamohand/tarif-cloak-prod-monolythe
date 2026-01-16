-- Migration pour ajouter le champ market_version aux tables organization et pending_registration

-- Ajouter market_version à la table organization
ALTER TABLE organization
ADD COLUMN IF NOT EXISTS market_version VARCHAR(10);

-- Ajouter market_version à la table pending_registration
ALTER TABLE pending_registration
ADD COLUMN IF NOT EXISTS market_version VARCHAR(10);

-- Créer un index pour améliorer les performances de recherche par market_version
CREATE INDEX IF NOT EXISTS idx_organization_market_version ON organization(market_version);
CREATE INDEX IF NOT EXISTS idx_pending_registration_market_version ON pending_registration(market_version);

