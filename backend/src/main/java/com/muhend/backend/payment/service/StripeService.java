package com.muhend.backend.payment.service;

import com.muhend.backend.invoice.dto.InvoiceDto;
import com.muhend.backend.invoice.service.InvoiceService;
import com.muhend.backend.organization.model.Organization;
import com.muhend.backend.organization.repository.OrganizationRepository;
import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.payment.config.StripeConfig;
import com.muhend.backend.payment.dto.CheckoutSessionResponse;
import com.muhend.backend.payment.dto.CreateCheckoutSessionRequest;
import com.muhend.backend.payment.repository.SubscriptionRepository;
import com.muhend.backend.pricing.service.PricingPlanService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les interactions avec Stripe.
 */
@Service
@Slf4j
public class StripeService {
    
    private final StripeConfig stripeConfig;
    private final PricingPlanService pricingPlanService;
    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceService invoiceService;
    
    @Value("${app.base-url:https://www.hscode.enclume-numerique.com}")
    private String baseUrl;
    
    public StripeService(
            StripeConfig stripeConfig,
            PricingPlanService pricingPlanService,
            OrganizationService organizationService,
            OrganizationRepository organizationRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceService invoiceService) {
        this.stripeConfig = stripeConfig;
        this.pricingPlanService = pricingPlanService;
        this.organizationService = organizationService;
        this.organizationRepository = organizationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceService = invoiceService;
        
        // Initialiser Stripe avec la clé secrète
        if (stripeConfig.isConfigured()) {
            Stripe.apiKey = stripeConfig.getSecretKey();
            log.info("Stripe initialisé avec succès");
        } else {
            log.warn("Stripe n'est pas configuré. Les clés API sont manquantes.");
        }
    }
    
    /**
     * Crée une session de checkout Stripe pour un plan tarifaire.
     */
    public CheckoutSessionResponse createCheckoutSession(
            Long organizationId,
            CreateCheckoutSessionRequest request) throws StripeException {
        
        if (!stripeConfig.isConfigured()) {
            throw new IllegalStateException("Stripe n'est pas configuré. Veuillez configurer les clés API.");
        }
        
        // Récupérer le plan tarifaire
        var pricingPlan = pricingPlanService.getPricingPlanById(request.getPricingPlanId());
        
        // Récupérer l'organisation
        var organization = organizationService.getOrganizationById(organizationId);
        
        // Créer ou récupérer le client Stripe
        String customerId = getOrCreateStripeCustomer(organizationId, organization.getEmail(), organization.getName());
        
        // Construire les paramètres de la session
        SessionCreateParams.Builder sessionParamsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(request.getSuccessUrl() != null ? request.getSuccessUrl() : baseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(request.getCancelUrl() != null ? request.getCancelUrl() : baseUrl + "/payment/cancel");
        
        // Ajouter les métadonnées
        sessionParamsBuilder.putMetadata("organization_id", organizationId.toString());
        sessionParamsBuilder.putMetadata("pricing_plan_id", request.getPricingPlanId().toString());
        if (request.getInvoiceId() != null) {
            sessionParamsBuilder.putMetadata("invoice_id", request.getInvoiceId().toString());
        }
        
        // Ajouter les items de la session
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        
        // Si c'est un plan mensuel
        if (pricingPlan.getPricePerMonth() != null && pricingPlan.getPricePerMonth().compareTo(BigDecimal.ZERO) > 0) {
            SessionCreateParams.LineItem.PriceData.Recurring recurring = SessionCreateParams.LineItem.PriceData.Recurring.builder()
                    .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                    .build();
            
            SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(stripeConfig.getCurrency().toLowerCase())
                    .setUnitAmount(pricingPlan.getPricePerMonth().multiply(BigDecimal.valueOf(100)).longValue()) // Convertir en centimes
                    .setRecurring(recurring)
                    .setProductData(
                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(pricingPlan.getName())
                                    .setDescription(pricingPlan.getDescription() != null ? pricingPlan.getDescription() : "")
                                    .build()
                    )
                    .build();
            
            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setPriceData(priceData)
                    .setQuantity(1L)
                    .build();
            
            lineItems.add(lineItem);
        } else {
            throw new IllegalArgumentException("Ce plan tarifaire n'est pas compatible avec les abonnements récurrents. Utilisez un plan avec un prix mensuel.");
        }
        
        // Ajouter les line items un par un
        for (SessionCreateParams.LineItem lineItem : lineItems) {
            sessionParamsBuilder.addLineItem(lineItem);
        }
        
        // Si c'est un plan d'essai, ajouter la période d'essai
        if (pricingPlan.getTrialPeriodDays() != null && pricingPlan.getTrialPeriodDays() > 0) {
            sessionParamsBuilder.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                            .setTrialPeriodDays((long) pricingPlan.getTrialPeriodDays())
                            .putMetadata("pricing_plan_id", request.getPricingPlanId().toString())
                            .build()
            );
        }
        
        // Créer la session
        Session session = Session.create(sessionParamsBuilder.build());
        
        log.info("Session de checkout Stripe créée: sessionId={}, organizationId={}, pricingPlanId={}",
                session.getId(), organizationId, request.getPricingPlanId());
        
        return new CheckoutSessionResponse(
                session.getId(),
                session.getUrl(),
                stripeConfig.getPublishableKey()
        );
    }
    
    /**
     * Crée une session de checkout pour payer une facture (paiement ponctuel).
     */
    public CheckoutSessionResponse createInvoiceCheckoutSession(
            Long organizationId,
            Long invoiceId,
            String successUrl,
            String cancelUrl) throws StripeException {
        
        if (!stripeConfig.isConfigured()) {
            throw new IllegalStateException("Stripe n'est pas configuré. Veuillez configurer les clés API.");
        }
        
        // Récupérer l'organisation et la facture
        var organization = organizationService.getOrganizationById(organizationId);
        InvoiceDto invoice = invoiceService.getInvoiceById(invoiceId);
        
        // Vérifier que la facture appartient à l'organisation
        if (!invoice.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("La facture n'appartient pas à cette organisation");
        }
        
        // Vérifier que la facture n'est pas déjà payée
        if (invoice.getStatus() == com.muhend.backend.invoice.model.Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Cette facture est déjà payée");
        }
        
        // Créer ou récupérer le client Stripe
        String customerId = getOrCreateStripeCustomer(organizationId, organization.getEmail(), organization.getName());
        
        // Construire les paramètres de la session (mode payment pour paiement ponctuel)
        SessionCreateParams.Builder sessionParamsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomer(customerId)
                .setSuccessUrl(successUrl != null ? successUrl : baseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl != null ? cancelUrl : baseUrl + "/payment/cancel");
        
        // Ajouter les métadonnées
        sessionParamsBuilder.putMetadata("organization_id", organizationId.toString());
        sessionParamsBuilder.putMetadata("invoice_id", invoiceId.toString());
        
        // Ajouter les items de la facture comme line items
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        
        // Créer un line item pour le montant total de la facture
        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency(stripeConfig.getCurrency().toLowerCase())
                .setUnitAmount(invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Convertir en centimes
                .setProductData(
                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName("Facture " + invoice.getInvoiceNumber())
                                .setDescription("Paiement de la facture " + invoice.getInvoiceNumber() + 
                                        " pour la période du " + invoice.getPeriodStart() + " au " + invoice.getPeriodEnd())
                                .build()
                )
                .build();
        
        SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                .setPriceData(priceData)
                .setQuantity(1L)
                .build();
        
        lineItems.add(lineItem);
        
        // Ajouter les line items
        for (SessionCreateParams.LineItem item : lineItems) {
            sessionParamsBuilder.addLineItem(item);
        }
        
        // Créer la session
        Session session = Session.create(sessionParamsBuilder.build());
        
        log.info("Session de checkout Stripe créée pour facture: sessionId={}, organizationId={}, invoiceId={}, amount={}",
                session.getId(), organizationId, invoiceId, invoice.getTotalAmount());
        
        return new CheckoutSessionResponse(
                session.getId(),
                session.getUrl(),
                stripeConfig.getPublishableKey()
        );
    }
    
    /**
     * Récupère ou crée un client Stripe pour une organisation.
     */
    private String getOrCreateStripeCustomer(Long organizationId, String email, String name) throws StripeException {
        // Vérifier si un client Stripe existe déjà pour cette organisation
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation introuvable: " + organizationId));
        
        if (organization.getStripeCustomerId() != null && !organization.getStripeCustomerId().isEmpty()) {
            try {
                // Vérifier que le client existe toujours chez Stripe
                Customer existingCustomer = Customer.retrieve(organization.getStripeCustomerId());
                log.debug("Client Stripe existant réutilisé: customerId={}, organizationId={}", 
                        existingCustomer.getId(), organizationId);
                return existingCustomer.getId();
            } catch (Exception e) {
                log.warn("Le client Stripe {} n'existe plus, création d'un nouveau client", 
                        organization.getStripeCustomerId(), e);
                // Le client n'existe plus, on va en créer un nouveau
            }
        }
        
        // Créer un nouveau client Stripe
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .putMetadata("organization_id", organizationId.toString())
                .build();
        
        Customer customer = Customer.create(params);
        log.info("Client Stripe créé: customerId={}, organizationId={}", customer.getId(), organizationId);
        
        // Sauvegarder l'ID du client dans l'organisation
        organization.setStripeCustomerId(customer.getId());
        organizationRepository.save(organization);
        
        return customer.getId();
    }
    
    /**
     * Récupère une session de checkout par son ID.
     */
    public Session getCheckoutSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
    
    /**
     * Annule un abonnement Stripe.
     * @param subscriptionId ID de l'abonnement local
     * @param cancelAtPeriodEnd Si true, l'abonnement sera annulé à la fin de la période. Si false, annulation immédiate.
     * @return L'abonnement Stripe mis à jour
     */
    public com.stripe.model.Subscription cancelSubscription(Long subscriptionId, boolean cancelAtPeriodEnd) throws StripeException {
        // Récupérer l'abonnement local
        com.muhend.backend.payment.model.Subscription subscription = 
                subscriptionRepository.findById(subscriptionId)
                        .orElseThrow(() -> new IllegalArgumentException("Abonnement introuvable: " + subscriptionId));
        
        if (subscription.getPaymentProviderSubscriptionId() == null) {
            throw new IllegalStateException("Cet abonnement n'a pas d'ID Stripe associé");
        }
        
        // Récupérer l'abonnement Stripe
        com.stripe.model.Subscription stripeSubscription = 
                com.stripe.model.Subscription.retrieve(subscription.getPaymentProviderSubscriptionId());
        
        // Annuler l'abonnement
        com.stripe.model.Subscription updatedSubscription;
        if (cancelAtPeriodEnd) {
            // Annulation à la fin de la période
            updatedSubscription = stripeSubscription.update(
                    com.stripe.param.SubscriptionUpdateParams.builder()
                            .setCancelAtPeriodEnd(true)
                            .build()
            );
            log.info("Abonnement Stripe programmé pour annulation à la fin de la période: subscriptionId={}", 
                    subscription.getId());
        } else {
            // Annulation immédiate
            updatedSubscription = stripeSubscription.cancel();
            log.info("Abonnement Stripe annulé immédiatement: subscriptionId={}", subscription.getId());
        }
        
        // Mettre à jour l'abonnement local
        subscription.setStatus(mapStripeSubscriptionStatus(updatedSubscription.getStatus()));
        subscription.setCancelAtPeriodEnd(updatedSubscription.getCancelAtPeriodEnd() != null && updatedSubscription.getCancelAtPeriodEnd());
        if (updatedSubscription.getCanceledAt() != null) {
            subscription.setCanceledAt(toLocalDateTime(updatedSubscription.getCanceledAt()));
        }
        subscriptionRepository.save(subscription);
        
        return updatedSubscription;
    }
    
    /**
     * Convertit un statut d'abonnement Stripe en statut local.
     */
    private com.muhend.backend.payment.model.Subscription.SubscriptionStatus mapStripeSubscriptionStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "trialing" -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.TRIALING;
            case "active" -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.ACTIVE;
            case "past_due" -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.PAST_DUE;
            case "canceled", "unpaid" -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.CANCELED;
            case "incomplete" -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.INCOMPLETE_EXPIRED;
            case "paused" -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.PAUSED;
            default -> com.muhend.backend.payment.model.Subscription.SubscriptionStatus.UNPAID;
        };
    }
    
    /**
     * Convertit un timestamp Unix en LocalDateTime.
     */
    private java.time.LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(timestamp), 
                java.time.ZoneId.systemDefault()
        );
    }
}

