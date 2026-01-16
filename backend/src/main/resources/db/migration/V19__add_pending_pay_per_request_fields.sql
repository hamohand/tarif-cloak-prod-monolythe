-- Migration: Ajout des champs pour gérer les changements vers Pay-per-Request en attente
-- V19__add_pending_pay_per_request_fields.sql

ALTER TABLE organization 
ADD COLUMN pending_pay_per_request_plan_id BIGINT,
ADD COLUMN pending_pay_per_request_change_date DATE;

-- Index pour optimiser les requêtes du scheduler
CREATE INDEX idx_org_pending_ppr_plan_id ON organization(pending_pay_per_request_plan_id) 
WHERE pending_pay_per_request_plan_id IS NOT NULL;

-- Commentaires pour documentation
COMMENT ON COLUMN organization.pending_pay_per_request_plan_id IS 'Plan Pay-per-Request en attente (prendra effet si quota dépassé immédiatement, sinon à la fin du cycle mensuel)';
COMMENT ON COLUMN organization.pending_pay_per_request_change_date IS 'Date à laquelle le changement vers Pay-per-Request prendra effet (fin du cycle si quota non dépassé)';






