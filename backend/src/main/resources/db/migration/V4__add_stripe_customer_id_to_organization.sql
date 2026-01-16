-- Migration pour ajouter la colonne stripe_customer_id à la table organization

ALTER TABLE organization 
ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255);

-- Index pour améliorer les performances des recherches
CREATE INDEX IF NOT EXISTS idx_organization_stripe_customer_id ON organization(stripe_customer_id);

