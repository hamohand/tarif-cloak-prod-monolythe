-- Script SQL pour ajouter les colonnes manquantes aux tables
-- À exécuter manuellement si les colonnes n'existent pas déjà

-- ============================================
-- Table pricing_plan
-- ============================================

-- Ajouter price_per_request si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'pricing_plan' AND column_name = 'price_per_request'
    ) THEN
        ALTER TABLE pricing_plan ADD COLUMN price_per_request NUMERIC(10, 2);
        RAISE NOTICE 'Colonne price_per_request ajoutée à pricing_plan';
    ELSE
        RAISE NOTICE 'Colonne price_per_request existe déjà dans pricing_plan';
    END IF;
END $$;

-- Ajouter trial_period_days si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'pricing_plan' AND column_name = 'trial_period_days'
    ) THEN
        ALTER TABLE pricing_plan ADD COLUMN trial_period_days INTEGER;
        RAISE NOTICE 'Colonne trial_period_days ajoutée à pricing_plan';
    ELSE
        RAISE NOTICE 'Colonne trial_period_days existe déjà dans pricing_plan';
    END IF;
END $$;

-- Modifier price_per_month pour qu'il soit nullable si ce n'est pas déjà le cas
-- IMPORTANT: Cette modification doit être faite AVANT d'insérer des données avec price_per_month = null
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'pricing_plan' 
        AND column_name = 'price_per_month' 
        AND is_nullable = 'NO'
    ) THEN
        -- Supprimer la contrainte NOT NULL
        ALTER TABLE pricing_plan ALTER COLUMN price_per_month DROP NOT NULL;
        RAISE NOTICE 'Contrainte NOT NULL supprimée de price_per_month';
    ELSE
        RAISE NOTICE 'Colonne price_per_month est déjà nullable';
    END IF;
END $$;

-- ============================================
-- Table organization
-- ============================================

-- Ajouter trial_expires_at si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'organization' AND column_name = 'trial_expires_at'
    ) THEN
        ALTER TABLE organization ADD COLUMN trial_expires_at TIMESTAMP;
        RAISE NOTICE 'Colonne trial_expires_at ajoutée à organization';
    ELSE
        RAISE NOTICE 'Colonne trial_expires_at existe déjà dans organization';
    END IF;
END $$;

