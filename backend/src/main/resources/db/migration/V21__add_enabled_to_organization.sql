-- Migration pour ajouter le champ enabled à la table organization
-- Permet de désactiver une organisation (interdire l'utilisation à tous ses collaborateurs)

ALTER TABLE organization ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;

-- Commentaire sur la colonne
COMMENT ON COLUMN organization.enabled IS 'false = organisation désactivée, aucun collaborateur ne peut utiliser application';
