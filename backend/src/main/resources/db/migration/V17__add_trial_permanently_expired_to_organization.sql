-- Script SQL pour ajouter la colonne trial_permanently_expired à la table organization
-- Migration V17 - Marquer si l'essai est définitivement terminé (ne peut plus être réactivé)

-- Ajouter la colonne trial_permanently_expired si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'organization' AND column_name = 'trial_permanently_expired'
    ) THEN
        ALTER TABLE organization ADD COLUMN trial_permanently_expired BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

