-- Migration: Ajout des champs pour gérer les cycles mensuels et les changements de plan en attente
-- V18__add_monthly_plan_cycle_fields.sql

ALTER TABLE organization 
ADD COLUMN monthly_plan_start_date DATE,
ADD COLUMN monthly_plan_end_date DATE,
ADD COLUMN pending_monthly_plan_id BIGINT,
ADD COLUMN pending_monthly_plan_change_date DATE,
ADD COLUMN last_pay_per_request_invoice_date DATE;

-- Index pour optimiser les requêtes du scheduler
CREATE INDEX idx_org_monthly_plan_end_date ON organization(monthly_plan_end_date) 
WHERE monthly_plan_end_date IS NOT NULL;

CREATE INDEX idx_org_pending_change_date ON organization(pending_monthly_plan_change_date) 
WHERE pending_monthly_plan_change_date IS NOT NULL;

-- Commentaires pour documentation
COMMENT ON COLUMN organization.monthly_plan_start_date IS 'Date de début du cycle mensuel actuel (du jour J au jour J-1 du mois suivant inclus)';
COMMENT ON COLUMN organization.monthly_plan_end_date IS 'Date de fin du cycle mensuel (inclus, réinitialisation le jour suivant)';
COMMENT ON COLUMN organization.pending_monthly_plan_id IS 'Plan mensuel en attente (prendra effet à la fin du cycle en cours)';
COMMENT ON COLUMN organization.pending_monthly_plan_change_date IS 'Date à laquelle le changement de plan prendra effet';
COMMENT ON COLUMN organization.last_pay_per_request_invoice_date IS 'Date de la dernière facture pay-per-request (pour facturer depuis cette date lors du passage vers mensuel)';






