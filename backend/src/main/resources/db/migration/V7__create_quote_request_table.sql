-- Migration pour créer la table quote_request

CREATE TABLE IF NOT EXISTS quote_request (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    contact_name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    responded_at TIMESTAMP
);

-- Ajouter la contrainte de clé étrangère si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_quote_request_organization'
    ) THEN
        ALTER TABLE quote_request 
        ADD CONSTRAINT fk_quote_request_organization 
        FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_quote_request_organization_id ON quote_request(organization_id);
CREATE INDEX IF NOT EXISTS idx_quote_request_status ON quote_request(status);
CREATE INDEX IF NOT EXISTS idx_quote_request_created_at ON quote_request(created_at);

