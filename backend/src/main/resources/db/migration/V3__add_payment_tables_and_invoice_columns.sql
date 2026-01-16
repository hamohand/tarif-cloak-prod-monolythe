-- Migration pour ajouter les tables de paiement et les colonnes nécessaires à la table invoice

-- Table subscription
CREATE TABLE IF NOT EXISTS subscription (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    pricing_plan_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NOT NULL,
    payment_provider_subscription_id VARCHAR(255),
    payment_provider_customer_id VARCHAR(255),
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    trial_start TIMESTAMP,
    trial_end TIMESTAMP,
    canceled_at TIMESTAMP,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_subscription_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_subscription_pricing_plan FOREIGN KEY (pricing_plan_id) REFERENCES pricing_plan(id)
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_subscription_organization_id ON subscription(organization_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON subscription(status);
CREATE INDEX IF NOT EXISTS idx_subscription_provider_subscription_id ON subscription(payment_provider_subscription_id);

-- Table payment
CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT,
    organization_id BIGINT NOT NULL,
    invoice_id BIGINT,
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(50) NOT NULL,
    payment_provider VARCHAR(50) NOT NULL,
    payment_provider_payment_id VARCHAR(255),
    payment_provider_payment_intent_id VARCHAR(255),
    payment_method VARCHAR(50),
    payment_method_type VARCHAR(50),
    description TEXT,
    failure_reason TEXT,
    invoice_url VARCHAR(500),
    receipt_url VARCHAR(500),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id),
    CONSTRAINT fk_payment_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id)
);

-- Index pour améliorer les performances
CREATE INDEX IF NOT EXISTS idx_payment_organization_id ON payment(organization_id);
CREATE INDEX IF NOT EXISTS idx_payment_subscription_id ON payment(subscription_id);
CREATE INDEX IF NOT EXISTS idx_payment_invoice_id ON payment(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(status);
CREATE INDEX IF NOT EXISTS idx_payment_provider_payment_id ON payment(payment_provider_payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_provider_payment_intent_id ON payment(payment_provider_payment_intent_id);

-- Ajouter les colonnes nécessaires à la table invoice (si elles n'existent pas déjà)
DO $$ 
BEGIN
    -- Colonne payment_id
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'invoice' AND column_name = 'payment_id') THEN
        ALTER TABLE invoice ADD COLUMN payment_id BIGINT;
        ALTER TABLE invoice ADD CONSTRAINT fk_invoice_payment 
            FOREIGN KEY (payment_id) REFERENCES payment(id);
    END IF;
    
    -- Colonne subscription_id
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'invoice' AND column_name = 'subscription_id') THEN
        ALTER TABLE invoice ADD COLUMN subscription_id BIGINT;
        ALTER TABLE invoice ADD CONSTRAINT fk_invoice_subscription 
            FOREIGN KEY (subscription_id) REFERENCES subscription(id);
    END IF;
    
    -- Colonne payment_provider
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'invoice' AND column_name = 'payment_provider') THEN
        ALTER TABLE invoice ADD COLUMN payment_provider VARCHAR(50);
    END IF;
    
    -- Colonne payment_provider_invoice_id
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'invoice' AND column_name = 'payment_provider_invoice_id') THEN
        ALTER TABLE invoice ADD COLUMN payment_provider_invoice_id VARCHAR(255);
    END IF;
    
    -- Colonne payment_intent_id
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'invoice' AND column_name = 'payment_intent_id') THEN
        ALTER TABLE invoice ADD COLUMN payment_intent_id VARCHAR(255);
    END IF;
    
    -- Colonne invoice_pdf_url
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'invoice' AND column_name = 'invoice_pdf_url') THEN
        ALTER TABLE invoice ADD COLUMN invoice_pdf_url VARCHAR(500);
    END IF;
END $$;

