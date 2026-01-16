-- Script SQL pour ajouter les nouvelles colonnes à la table pricing_plan
-- Si les colonnes existent déjà, les commandes ALTER TABLE échoueront silencieusement
-- ou peuvent être ignorées selon la configuration PostgreSQL

-- Ajouter la colonne price_per_request si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'pricing_plan' AND column_name = 'price_per_request'
    ) THEN
        ALTER TABLE pricing_plan ADD COLUMN price_per_request NUMERIC(10, 2);
    END IF;
END $$;

-- Ajouter la colonne trial_period_days si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'pricing_plan' AND column_name = 'trial_period_days'
    ) THEN
        ALTER TABLE pricing_plan ADD COLUMN trial_period_days INTEGER;
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
        ALTER TABLE pricing_plan ALTER COLUMN price_per_month DROP NOT NULL;
        RAISE NOTICE 'Contrainte NOT NULL supprimée de price_per_month';
    ELSE
        RAISE NOTICE 'price_per_month est déjà nullable';
    END IF;
END $$;

