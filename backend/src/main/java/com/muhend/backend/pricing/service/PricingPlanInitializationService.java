package com.muhend.backend.pricing.service;

import com.muhend.backend.pricing.model.PricingPlan;
import com.muhend.backend.pricing.repository.PricingPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service pour initialiser les plans tarifaires par défaut.
 * S'exécute après la correction du schéma de la table.
 */
@Service
@Slf4j
@Order(2)
public class PricingPlanInitializationService {
    
    @Autowired
    private PricingPlanRepository pricingPlanRepository;
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializePricingPlans() {
        log.info("Vérification et initialisation des plans tarifaires par défaut...");
        
        // Plan d'essai gratuit
        if (!pricingPlanRepository.existsByName("Essai gratuit")) {
            PricingPlan trial = new PricingPlan();
            trial.setName("Essai gratuit");
            trial.setDescription("20 requêtes gratuites valables pendant une semaine");
            trial.setPricePerMonth(BigDecimal.ZERO);
            trial.setPricePerRequest(null);
            trial.setMonthlyQuota(20);
            trial.setTrialPeriodDays(7);
            trial.setFeatures("20 requêtes gratuites, Valable 7 jours, Accès à toutes les fonctionnalités");
            trial.setIsActive(true);
            trial.setDisplayOrder(0);
            pricingPlanRepository.save(trial);
            log.info("Plan 'Essai gratuit' créé");
        }
        
        // Plan facturé à la requête
        if (!pricingPlanRepository.existsByName("Pay-per-Request")) {
            PricingPlan payPerRequest = new PricingPlan();
            payPerRequest.setName("Pay-per-Request");
            payPerRequest.setDescription("Facturation à la requête sans limite de temps");
            payPerRequest.setPricePerMonth(null);
            payPerRequest.setPricePerRequest(new BigDecimal("0.05")); // 5 centimes par requête
            payPerRequest.setMonthlyQuota(null); // Pas de quota mensuel
            payPerRequest.setTrialPeriodDays(null);
            payPerRequest.setFeatures("Facturation à la requête, Pas de limite de temps, Pas de quota mensuel, Support par email");
            payPerRequest.setIsActive(true);
            payPerRequest.setDisplayOrder(1);
            pricingPlanRepository.save(payPerRequest);
            log.info("Plan 'Pay-per-Request' créé");
        }
        
        // Plan Starter
        if (!pricingPlanRepository.existsByName("Starter")) {
            PricingPlan starter = new PricingPlan();
            starter.setName("Starter");
            starter.setDescription("Plan de démarrage idéal pour les petites entreprises");
            starter.setPricePerMonth(new BigDecimal("29.99"));
            starter.setPricePerRequest(null);
            starter.setMonthlyQuota(1000);
            starter.setTrialPeriodDays(null);
            starter.setFeatures("1000 requêtes/mois, Support par email, Accès à toutes les fonctionnalités de base");
            starter.setIsActive(true);
            starter.setDisplayOrder(2);
            pricingPlanRepository.save(starter);
            log.info("Plan 'Starter' créé");
        }
        
        // Plan Professional
        if (!pricingPlanRepository.existsByName("Professional")) {
            PricingPlan professional = new PricingPlan();
            professional.setName("Professional");
            professional.setDescription("Plan professionnel pour les entreprises en croissance");
            professional.setPricePerMonth(new BigDecimal("79.99"));
            professional.setPricePerRequest(null);
            professional.setMonthlyQuota(5000);
            professional.setTrialPeriodDays(null);
            professional.setFeatures("5000 requêtes/mois, Support prioritaire, Accès à toutes les fonctionnalités, Rapports détaillés");
            professional.setIsActive(true);
            professional.setDisplayOrder(3);
            pricingPlanRepository.save(professional);
            log.info("Plan 'Professional' créé");
        }
        
        // Plan Enterprise
        if (!pricingPlanRepository.existsByName("Enterprise")) {
            PricingPlan enterprise = new PricingPlan();
            enterprise.setName("Enterprise");
            enterprise.setDescription("Plan entreprise avec quota illimité");
            enterprise.setPricePerMonth(new BigDecimal("199.99"));
            enterprise.setPricePerRequest(null);
            enterprise.setMonthlyQuota(null); // Illimité
            enterprise.setTrialPeriodDays(null);
            enterprise.setFeatures("Quota illimité, Support dédié 24/7, Toutes les fonctionnalités, Rapports avancés, Personnalisation");
            enterprise.setIsActive(true);
            enterprise.setDisplayOrder(4);
            pricingPlanRepository.save(enterprise);
            log.info("Plan 'Enterprise' créé");
        }
        
        log.info("Vérification des plans tarifaires terminée");
    }
}

