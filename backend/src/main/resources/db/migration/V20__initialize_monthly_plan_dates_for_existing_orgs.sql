-- Migration pour initialiser les dates de cycle mensuel pour les organisations existantes
-- qui ont un plan mensuel (pricePerMonth > 0) mais pas de monthlyPlanEndDate

-- Mettre à jour les organisations avec un plan mensuel qui n'ont pas de dates de cycle
UPDATE organization o
SET
    monthly_plan_start_date = COALESCE(o.monthly_plan_start_date, CURRENT_DATE),
    monthly_plan_end_date = COALESCE(o.monthly_plan_end_date, CURRENT_DATE + INTERVAL '1 month' - INTERVAL '1 day')
WHERE o.pricing_plan_id IS NOT NULL
  AND o.monthly_plan_end_date IS NULL
  AND EXISTS (
      SELECT 1 FROM pricing_plan pp
      WHERE pp.id = o.pricing_plan_id
        AND pp.price_per_month IS NOT NULL
        AND pp.price_per_month > 0
        AND (pp.trial_period_days IS NULL OR pp.trial_period_days = 0)
  );
