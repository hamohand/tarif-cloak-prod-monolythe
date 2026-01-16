-- Script SQL pour créer la table pricing_plan avec toutes les colonnes nullable nécessaires
-- Ce script est exécuté avant que Hibernate ne tente de créer/modifier la table

-- Supprimer la table si elle existe déjà
DROP TABLE IF EXISTS pricing_plan CASCADE;

-- Créer la table pricing_plan avec price_per_month nullable
CREATE TABLE pricing_plan (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    price_per_month NUMERIC(10, 2) NULL,  -- Explicitement NULL pour permettre les plans facturés à la requête
    price_per_request NUMERIC(10, 2) NULL, -- Explicitement NULL pour permettre les plans mensuels
    monthly_quota INTEGER NULL,
    trial_period_days INTEGER NULL,
    features TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

-- Vérifier que price_per_month est bien nullable
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'pricing_plan' 
        AND column_name = 'price_per_month' 
        AND is_nullable = 'NO'
    ) THEN
        RAISE EXCEPTION 'La colonne price_per_month doit être nullable';
    END IF;
END $$;

-- Insertion de plans tarifaires par défaut
INSERT INTO pricing_plan (name, description, price_per_month, monthly_quota, features, is_active, display_order) VALUES
('Starter', 'Plan de démarrage idéal pour les petites entreprises', 29.99, 1000, '1000 requêtes/mois, Support par email, Accès à toutes les fonctionnalités de base', true, 1),
('Professional', 'Plan professionnel pour les entreprises en croissance', 79.99, 5000, '5000 requêtes/mois, Support prioritaire, Accès à toutes les fonctionnalités, Rapports détaillés', true, 2),
('Enterprise', 'Plan entreprise avec quota illimité', 199.99, NULL, 'Quota illimité, Support dédié 24/7, Toutes les fonctionnalités, Rapports avancés, Personnalisation', true, 3);

