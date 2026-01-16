-- Migration pour ajouter le champ organization_activity_domain à la table pending_registration

-- Ajouter la colonne organization_activity_domain si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'pending_registration' 
        AND column_name = 'organization_activity_domain'
    ) THEN
        ALTER TABLE pending_registration 
        ADD COLUMN organization_activity_domain VARCHAR(255) NULL;
        
        RAISE NOTICE 'Colonne organization_activity_domain ajoutée à pending_registration';
    ELSE
        RAISE NOTICE 'Colonne organization_activity_domain existe déjà dans pending_registration';
    END IF;
END $$;

