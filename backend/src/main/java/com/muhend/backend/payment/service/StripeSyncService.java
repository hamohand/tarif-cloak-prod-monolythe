package com.muhend.backend.payment.service;

import com.muhend.backend.organization.model.Organization;
import com.muhend.backend.organization.repository.OrganizationRepository;
import com.muhend.backend.payment.config.StripeConfig;
import com.muhend.backend.payment.model.Subscription;
import com.muhend.backend.payment.repository.SubscriptionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.SubscriptionCollection;
import com.stripe.param.SubscriptionListParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Service pour synchroniser les données avec Stripe.
 * Permet de récupérer les abonnements et paiements depuis Stripe et les synchroniser avec la base de données locale.
 */
@Service
@Slf4j
public class StripeSyncService {
    
    private final StripeConfig stripeConfig;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    
    public StripeSyncService(
            StripeConfig stripeConfig,
            SubscriptionRepository subscriptionRepository,
            OrganizationRepository organizationRepository) {
        this.stripeConfig = stripeConfig;
        this.subscriptionRepository = subscriptionRepository;
        this.organizationRepository = organizationRepository;
        
        if (stripeConfig.isConfigured()) {
            Stripe.apiKey = stripeConfig.getSecretKey();
        }
    }
    
    /**
     * Synchronise tous les abonnements Stripe avec la base de données locale.
     * Récupère tous les abonnements actifs depuis Stripe et les met à jour dans la base de données.
     */
    @Transactional
    public int syncAllSubscriptions() throws StripeException {
        if (!stripeConfig.isConfigured()) {
            throw new IllegalStateException("Stripe n'est pas configuré");
        }
        
        log.info("Démarrage de la synchronisation des abonnements Stripe...");
        
        int syncedCount = 0;
        SubscriptionListParams params = SubscriptionListParams.builder()
                .setLimit(100L) // Récupérer 100 abonnements à la fois
                .build();
        
        SubscriptionCollection subscriptions = com.stripe.model.Subscription.list(params);
        
        for (com.stripe.model.Subscription stripeSubscription : subscriptions.getData()) {
            try {
                syncSubscription(stripeSubscription);
                syncedCount++;
            } catch (Exception e) {
                log.error("Erreur lors de la synchronisation de l'abonnement Stripe: {}", 
                        stripeSubscription.getId(), e);
            }
        }
        
        log.info("Synchronisation terminée: {} abonnement(s) synchronisé(s)", syncedCount);
        return syncedCount;
    }
    
    /**
     * Synchronise un abonnement Stripe spécifique avec la base de données locale.
     */
    @Transactional
    public Subscription syncSubscription(com.stripe.model.Subscription stripeSubscription) {
        // Récupérer les métadonnées
        String organizationIdStr = stripeSubscription.getMetadata().get("organization_id");
        String pricingPlanIdStr = stripeSubscription.getMetadata().get("pricing_plan_id");
        
        if (organizationIdStr == null) {
            log.warn("Abonnement Stripe sans organization_id dans les métadonnées: {}", stripeSubscription.getId());
            return null;
        }
        
        Long organizationId = Long.parseLong(organizationIdStr);
        Long pricingPlanId = pricingPlanIdStr != null ? Long.parseLong(pricingPlanIdStr) : null;
        
        // Vérifier si l'abonnement existe déjà
        Subscription subscription = subscriptionRepository
                .findByPaymentProviderSubscriptionId(stripeSubscription.getId())
                .orElseGet(() -> {
                    Subscription newSubscription = new Subscription();
                    newSubscription.setOrganizationId(organizationId);
                    newSubscription.setPricingPlanId(pricingPlanId);
                    newSubscription.setPaymentProvider("stripe");
                    newSubscription.setPaymentProviderSubscriptionId(stripeSubscription.getId());
                    newSubscription.setPaymentProviderCustomerId(stripeSubscription.getCustomer());
                    return newSubscription;
                });
        
        // Mettre à jour les informations
        subscription.setStatus(mapStripeSubscriptionStatus(stripeSubscription.getStatus()));
        subscription.setCurrentPeriodStart(toLocalDateTime(stripeSubscription.getCurrentPeriodStart()));
        subscription.setCurrentPeriodEnd(toLocalDateTime(stripeSubscription.getCurrentPeriodEnd()));
        
        if (stripeSubscription.getTrialStart() != null) {
            subscription.setTrialStart(toLocalDateTime(stripeSubscription.getTrialStart()));
        }
        if (stripeSubscription.getTrialEnd() != null) {
            subscription.setTrialEnd(toLocalDateTime(stripeSubscription.getTrialEnd()));
        }
        if (stripeSubscription.getCanceledAt() != null) {
            subscription.setCanceledAt(toLocalDateTime(stripeSubscription.getCanceledAt()));
        }
        
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd() != null && stripeSubscription.getCancelAtPeriodEnd());
        
        subscription = subscriptionRepository.save(subscription);
        log.debug("Abonnement synchronisé: id={}, organizationId={}", subscription.getId(), organizationId);
        
        return subscription;
    }
    
    /**
     * Synchronise les abonnements d'une organisation spécifique.
     */
    @Transactional
    public int syncOrganizationSubscriptions(Long organizationId) throws StripeException {
        if (!stripeConfig.isConfigured()) {
            throw new IllegalStateException("Stripe n'est pas configuré");
        }
        
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation introuvable: " + organizationId));
        
        if (organization.getStripeCustomerId() == null || organization.getStripeCustomerId().isEmpty()) {
            log.warn("L'organisation {} n'a pas de client Stripe associé", organizationId);
            return 0;
        }
        
        // Récupérer le client Stripe
        Customer customer = Customer.retrieve(organization.getStripeCustomerId());
        
        // Récupérer les abonnements du client
        SubscriptionListParams params = SubscriptionListParams.builder()
                .setCustomer(customer.getId())
                .setLimit(100L)
                .build();
        
        SubscriptionCollection subscriptions = com.stripe.model.Subscription.list(params);
        
        int syncedCount = 0;
        for (com.stripe.model.Subscription stripeSubscription : subscriptions.getData()) {
            try {
                syncSubscription(stripeSubscription);
                syncedCount++;
            } catch (Exception e) {
                log.error("Erreur lors de la synchronisation de l'abonnement: {}", 
                        stripeSubscription.getId(), e);
            }
        }
        
        log.info("Synchronisation terminée pour l'organisation {}: {} abonnement(s) synchronisé(s)", 
                organizationId, syncedCount);
        
        return syncedCount;
    }
    
    /**
     * Convertit un statut d'abonnement Stripe en statut local.
     */
    private Subscription.SubscriptionStatus mapStripeSubscriptionStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "trialing" -> Subscription.SubscriptionStatus.TRIALING;
            case "active" -> Subscription.SubscriptionStatus.ACTIVE;
            case "past_due" -> Subscription.SubscriptionStatus.PAST_DUE;
            case "canceled", "unpaid" -> Subscription.SubscriptionStatus.CANCELED;
            case "incomplete" -> Subscription.SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> Subscription.SubscriptionStatus.INCOMPLETE_EXPIRED;
            case "paused" -> Subscription.SubscriptionStatus.PAUSED;
            default -> Subscription.SubscriptionStatus.UNPAID;
        };
    }
    
    /**
     * Convertit un timestamp Unix en LocalDateTime.
     */
    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }
}

