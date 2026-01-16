-- Migration pour s'assurer que tous les plans existants ont market_version = 'DEFAULT'
-- sauf ceux qui ont déjà market_version = 'DZ' ou une autre valeur spécifique

-- Mettre à jour les plans qui n'ont pas de market_version ou qui ont NULL
UPDATE pricing_plan 
SET market_version = 'DEFAULT' 
WHERE market_version IS NULL 
   OR market_version = '';

-- S'assurer que les plans par défaut (Starter, Professional, Enterprise) ont market_version = 'DEFAULT'
UPDATE pricing_plan 
SET market_version = 'DEFAULT' 
WHERE name IN ('Starter', 'Professional', 'Enterprise')
  AND (market_version IS NULL OR market_version != 'DEFAULT');

-- Vérification : tous les plans actifs doivent avoir un market_version non NULL
-- (Cette vérification peut être commentée si on veut permettre NULL pour certains cas)
-- DO $$
-- BEGIN
--     IF EXISTS (
--         SELECT 1 FROM pricing_plan 
--         WHERE is_active = true 
--         AND (market_version IS NULL OR market_version = '')
--     ) THEN
--         RAISE WARNING 'Des plans actifs ont market_version NULL ou vide';
--     END IF;
-- END $$;

