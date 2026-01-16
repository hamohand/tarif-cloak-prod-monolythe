-- Script SQL pour ajouter la colonne trial_expires_at à la table organization
-- Migration V3_1 - Exécutée après V3__add_payment_tables_and_invoice_columns
-- Si la colonne existe déjà, la commande ALTER TABLE échouera silencieusement

-- Ajouter la colonne trial_expires_at si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'organization' AND column_name = 'trial_expires_at'
    ) THEN
        ALTER TABLE organization ADD COLUMN trial_expires_at TIMESTAMP;
    END IF;
END $$;

