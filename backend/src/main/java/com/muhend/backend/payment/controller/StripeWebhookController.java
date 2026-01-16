package com.muhend.backend.payment.controller;

import com.muhend.backend.payment.config.StripeConfig;
import com.muhend.backend.invoice.model.Invoice;
import com.muhend.backend.invoice.repository.InvoiceRepository;
import com.muhend.backend.organization.model.Organization;
import com.muhend.backend.organization.repository.OrganizationRepository;
import com.muhend.backend.payment.model.Payment;
import com.muhend.backend.payment.model.Subscription;
import com.muhend.backend.payment.repository.PaymentRepository;
import com.muhend.backend.payment.repository.SubscriptionRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Controller pour gérer les webhooks Stripe.
 * Les webhooks permettent à Stripe de notifier l'application des événements de paiement.
 */
@RestController
@RequestMapping("/webhooks/stripe")
@Slf4j
public class StripeWebhookController {
    
    private final StripeConfig stripeConfig;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    
    public StripeWebhookController(
            StripeConfig stripeConfig,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            OrganizationRepository organizationRepository) {
        this.stripeConfig = stripeConfig;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
    }
    
    /**
     * Endpoint pour recevoir les webhooks Stripe.
     * Cet endpoint doit être public (non authentifié) car Stripe envoie les webhooks directement.
     * La sécurité est assurée par la vérification de la signature Stripe.
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        if (!stripeConfig.isConfigured() || stripeConfig.getWebhookSecret().isEmpty()) {
            log.error("Stripe webhook secret n'est pas configuré");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook secret not configured");
        }
        
        Event event;
        
        try {
            // Vérifier la signature du webhook
            event = Webhook.constructEvent(
                    payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Erreur de vérification de signature du webhook Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Erreur lors du traitement du webhook Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error processing webhook");
        }
        
        // Traiter l'événement
        log.info("Webhook Stripe reçu: type={}, id={}", event.getType(), event.getId());
        
        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                default:
                    log.debug("Événement Stripe non géré: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement Stripe: {}", event.getType(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing event");
        }
        
        return ResponseEntity.ok("Webhook processed successfully");
    }
    
    /**
     * Gère l'événement checkout.session.completed.
     * Se déclenche quand un utilisateur complète le checkout.
     */
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            log.warn("Session null dans checkout.session.completed");
            return;
        }
        
        log.info("Checkout session complétée: sessionId={}, customerId={}, subscriptionId={}",
                session.getId(), session.getCustomer(), session.getSubscription());
        
        // Si c'est un abonnement, il sera créé/mis à jour par l'événement customer.subscription.created/updated
        // Si c'est un paiement ponctuel, il sera géré par payment_intent.succeeded
    }
    
    /**
     * Gère les événements customer.subscription.created et customer.subscription.updated.
     */
    private void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSubscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSubscription == null) {
            log.warn("Subscription null dans customer.subscription.updated");
            return;
        }
        
        log.info("Abonnement Stripe créé/mis à jour: subscriptionId={}, status={}",
                stripeSubscription.getId(), stripeSubscription.getStatus());
        
        // Récupérer les métadonnées
        Map<String, String> metadata = stripeSubscription.getMetadata();
        String organizationIdStr = metadata.get("organization_id");
        String pricingPlanIdStr = metadata.get("pricing_plan_id");
        
        if (organizationIdStr == null || pricingPlanIdStr == null) {
            log.warn("Métadonnées manquantes dans l'abonnement Stripe: organizationId={}, pricingPlanId={}",
                    organizationIdStr, pricingPlanIdStr);
            return;
        }
        
        Long organizationId = Long.parseLong(organizationIdStr);
        Long pricingPlanId = Long.parseLong(pricingPlanIdStr);
        
        // Vérifier si l'abonnement existe déjà
        Subscription subscription = subscriptionRepository
                .findByPaymentProviderSubscriptionId(stripeSubscription.getId())
                .orElse(null);
        
        if (subscription == null) {
            // Créer un nouvel abonnement
            subscription = new Subscription();
            subscription.setOrganizationId(organizationId);
            subscription.setPricingPlanId(pricingPlanId);
            subscription.setPaymentProvider("stripe");
            subscription.setPaymentProviderSubscriptionId(stripeSubscription.getId());
            subscription.setPaymentProviderCustomerId(stripeSubscription.getCustomer());
        }
        
        // Mettre à jour les informations
        subscription.setStatus(mapStripeSubscriptionStatus(stripeSubscription.getStatus()));
        subscription.setCurrentPeriodStart(toLocalDateTime(stripeSubscription.getCurrentPeriodStart()));
        subscription.setCurrentPeriodEnd(toLocalDateTime(stripeSubscription.getCurrentPeriodEnd()));
        
        if (stripeSubscription.getTrialEnd() != null) {
            subscription.setTrialStart(toLocalDateTime(stripeSubscription.getTrialStart()));
            subscription.setTrialEnd(toLocalDateTime(stripeSubscription.getTrialEnd()));
        }
        
        if (stripeSubscription.getCanceledAt() != null) {
            subscription.setCanceledAt(toLocalDateTime(stripeSubscription.getCanceledAt()));
        }
        
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd() != null && stripeSubscription.getCancelAtPeriodEnd());
        
        subscriptionRepository.save(subscription);
        log.info("Abonnement sauvegardé: id={}, organizationId={}", subscription.getId(), organizationId);
    }
    
    /**
     * Gère l'événement customer.subscription.deleted.
     */
    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSubscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeSubscription == null) {
            return;
        }
        
        subscriptionRepository.findByPaymentProviderSubscriptionId(stripeSubscription.getId())
                .ifPresent(subscription -> {
                    subscription.setStatus(Subscription.SubscriptionStatus.CANCELED);
                    subscription.setCanceledAt(LocalDateTime.now());
                    subscriptionRepository.save(subscription);
                    log.info("Abonnement annulé: id={}", subscription.getId());
                });
    }
    
    /**
     * Gère l'événement invoice.payment_succeeded.
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        com.stripe.model.Invoice stripeInvoice = 
                (com.stripe.model.Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeInvoice == null) {
            return;
        }
        
        log.info("Paiement de facture réussi: invoiceId={}, amount={}, subscriptionId={}",
                stripeInvoice.getId(), stripeInvoice.getAmountPaid(), stripeInvoice.getSubscription());
        
        // Récupérer l'organisation via le customer
        String customerId = stripeInvoice.getCustomer();
        Organization organization = organizationRepository.findByStripeCustomerId(customerId)
                .orElse(null);
        
        if (organization == null) {
            log.warn("Organisation introuvable pour le client Stripe: {}", customerId);
            return;
        }
        
        // Récupérer ou créer le paiement
        Payment payment = paymentRepository
                .findByPaymentProviderPaymentId(stripeInvoice.getCharge())
                .orElseGet(() -> {
                    Payment newPayment = new Payment();
                    newPayment.setOrganizationId(organization.getId());
                    newPayment.setPaymentProvider("stripe");
                    newPayment.setCurrency(stripeInvoice.getCurrency().toUpperCase());
                    return newPayment;
                });
        
        // Mettre à jour les informations du paiement
        payment.setAmount(BigDecimal.valueOf(stripeInvoice.getAmountPaid()).divide(BigDecimal.valueOf(100))); // Convertir de centimes
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment.setPaymentProviderPaymentId(stripeInvoice.getCharge() != null ? stripeInvoice.getCharge() : "");
        payment.setPaymentProviderPaymentIntentId(stripeInvoice.getPaymentIntent() != null ? stripeInvoice.getPaymentIntent() : "");
        payment.setDescription("Paiement d'abonnement - " + stripeInvoice.getNumber());
        payment.setInvoiceUrl(stripeInvoice.getHostedInvoiceUrl());
        // Note: getReceiptUrl() n'existe pas dans l'API Stripe Invoice, on utilise hostedInvoiceUrl à la place
        payment.setReceiptUrl(stripeInvoice.getHostedInvoiceUrl());
        if (stripeInvoice.getStatusTransitions() != null && stripeInvoice.getStatusTransitions().getPaidAt() != null) {
            payment.setPaidAt(toLocalDateTime(stripeInvoice.getStatusTransitions().getPaidAt()));
        } else {
            payment.setPaidAt(LocalDateTime.now());
        }
        
        // Lier à l'abonnement si disponible
        if (stripeInvoice.getSubscription() != null) {
            subscriptionRepository.findByPaymentProviderSubscriptionId(stripeInvoice.getSubscription())
                    .ifPresent(sub -> payment.setSubscriptionId(sub.getId()));
        }
        
        paymentRepository.save(payment);
        log.info("Paiement enregistré: id={}, organizationId={}, amount={}", 
                payment.getId(), organization.getId(), payment.getAmount());
        
        // Mettre à jour le statut de la facture locale si elle existe
        if (stripeInvoice.getMetadata() != null && stripeInvoice.getMetadata().containsKey("invoice_id")) {
            try {
                Long invoiceId = Long.parseLong(stripeInvoice.getMetadata().get("invoice_id"));
                invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
                    invoice.setStatus(Invoice.InvoiceStatus.PAID);
                    invoice.setPaidAt(LocalDateTime.now());
                    invoice.setPaymentId(payment.getId());
                    invoice.setPaymentProvider("stripe");
                    invoice.setPaymentProviderInvoiceId(stripeInvoice.getId());
                    invoiceRepository.save(invoice);
                    log.info("Facture locale mise à jour: invoiceId={}", invoiceId);
                });
            } catch (NumberFormatException e) {
                log.warn("Impossible de parser invoice_id depuis les métadonnées Stripe", e);
            }
        }
    }
    
    /**
     * Gère l'événement invoice.payment_failed.
     */
    private void handleInvoicePaymentFailed(Event event) {
        com.stripe.model.Invoice stripeInvoice = 
                (com.stripe.model.Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
        if (stripeInvoice == null) {
            return;
        }
        
        log.warn("Échec du paiement de facture: invoiceId={}, subscriptionId={}", 
                stripeInvoice.getId(), stripeInvoice.getSubscription());
        
        // Mettre à jour le statut de l'abonnement en PAST_DUE
        if (stripeInvoice.getSubscription() != null) {
            subscriptionRepository.findByPaymentProviderSubscriptionId(stripeInvoice.getSubscription())
                    .ifPresent(subscription -> {
                        subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
                        subscriptionRepository.save(subscription);
                        log.info("Abonnement mis à jour en PAST_DUE: subscriptionId={}", subscription.getId());
                    });
        }
        
        // Enregistrer l'échec du paiement
        String customerId = stripeInvoice.getCustomer();
        Organization organization = organizationRepository.findByStripeCustomerId(customerId)
                .orElse(null);
        
        if (organization != null) {
            Payment payment = new Payment();
            payment.setOrganizationId(organization.getId());
            payment.setAmount(BigDecimal.valueOf(stripeInvoice.getAmountDue()).divide(BigDecimal.valueOf(100)));
            payment.setCurrency(stripeInvoice.getCurrency().toUpperCase());
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setPaymentProvider("stripe");
            payment.setDescription("Échec du paiement - " + stripeInvoice.getNumber());
            // Note: getLastPaymentError() n'existe pas dans l'API Stripe Invoice
            // On peut utiliser attemptCount ou d'autres informations disponibles
            payment.setFailureReason("Paiement échoué pour la facture " + stripeInvoice.getNumber());
            
            if (stripeInvoice.getSubscription() != null) {
                subscriptionRepository.findByPaymentProviderSubscriptionId(stripeInvoice.getSubscription())
                        .ifPresent(sub -> payment.setSubscriptionId(sub.getId()));
            }
            
            paymentRepository.save(payment);
            log.info("Échec de paiement enregistré: id={}, organizationId={}", 
                    payment.getId(), organization.getId());
        }
        
        // TODO: Notifier l'organisation par email
    }
    
    /**
     * Gère l'événement payment_intent.succeeded.
     */
    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = 
                (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent == null) {
            return;
        }
        
        log.info("Paiement réussi: paymentIntentId={}, amount={}, customerId={}",
                paymentIntent.getId(), paymentIntent.getAmount(), paymentIntent.getCustomer());
        
        // Récupérer l'organisation via le customer
        String customerId = paymentIntent.getCustomer();
        if (customerId == null) {
            log.warn("Customer ID manquant dans payment_intent.succeeded");
            return;
        }
        
        Organization organization = organizationRepository.findByStripeCustomerId(customerId)
                .orElse(null);
        
        if (organization == null) {
            log.warn("Organisation introuvable pour le client Stripe: {}", customerId);
            return;
        }
        
        // Récupérer ou créer le paiement
        Payment payment = paymentRepository
                .findByPaymentProviderPaymentIntentId(paymentIntent.getId())
                .orElseGet(() -> {
                    Payment newPayment = new Payment();
                    newPayment.setOrganizationId(organization.getId());
                    newPayment.setPaymentProvider("stripe");
                    newPayment.setCurrency(paymentIntent.getCurrency().toUpperCase());
                    return newPayment;
                });
        
        // Mettre à jour les informations du paiement
        payment.setAmount(BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100))); // Convertir de centimes
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment.setPaymentProviderPaymentIntentId(paymentIntent.getId());
        payment.setPaymentMethod(paymentIntent.getPaymentMethod() != null ? paymentIntent.getPaymentMethod() : "card");
        payment.setDescription(paymentIntent.getDescription() != null ? paymentIntent.getDescription() : "Paiement ponctuel");
        payment.setPaidAt(LocalDateTime.now());
        
        paymentRepository.save(payment);
        log.info("Paiement enregistré: id={}, organizationId={}, amount={}", 
                payment.getId(), organization.getId(), payment.getAmount());
    }
    
    /**
     * Gère l'événement payment_intent.payment_failed.
     */
    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = 
                (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent == null) {
            return;
        }
        
        log.warn("Échec du paiement: paymentIntentId={}, customerId={}", 
                paymentIntent.getId(), paymentIntent.getCustomer());
        
        // Récupérer l'organisation via le customer
        String customerId = paymentIntent.getCustomer();
        if (customerId == null) {
            return;
        }
        
        Organization organization = organizationRepository.findByStripeCustomerId(customerId)
                .orElse(null);
        
        if (organization == null) {
            return;
        }
        
        // Enregistrer l'échec du paiement
        Payment payment = new Payment();
        payment.setOrganizationId(organization.getId());
        payment.setAmount(BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100)));
        payment.setCurrency(paymentIntent.getCurrency().toUpperCase());
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setPaymentProvider("stripe");
        payment.setPaymentProviderPaymentIntentId(paymentIntent.getId());
        payment.setDescription("Échec du paiement");
        payment.setFailureReason(paymentIntent.getLastPaymentError() != null ? 
                paymentIntent.getLastPaymentError().getMessage() : "Paiement échoué");
        
        paymentRepository.save(payment);
        log.info("Échec de paiement enregistré: id={}, organizationId={}", 
                payment.getId(), organization.getId());
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

