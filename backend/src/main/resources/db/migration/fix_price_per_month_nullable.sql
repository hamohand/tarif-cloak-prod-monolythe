-- Script SQL pour corriger la contrainte NOT NULL sur price_per_month
-- À exécuter immédiatement pour permettre les plans facturés à la requête

-- Supprimer la contrainte NOT NULL de price_per_month
ALTER TABLE pricing_plan ALTER COLUMN price_per_month DROP NOT NULL;

-- Vérifier que la modification a été appliquée
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'pricing_plan' AND column_name = 'price_per_month';

