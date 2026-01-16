-- Migration pour ajouter le champ activity_domain à la table organization

-- Ajouter la colonne activity_domain si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'organization' 
        AND column_name = 'activity_domain'
    ) THEN
        ALTER TABLE organization 
        ADD COLUMN activity_domain VARCHAR(255) NULL;
        
        RAISE NOTICE 'Colonne activity_domain ajoutée à organization';
    ELSE
        RAISE NOTICE 'Colonne activity_domain existe déjà dans organization';
    END IF;
END $$;

