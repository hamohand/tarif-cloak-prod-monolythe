package com.muhend.backend.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant un abonnement à un plan tarifaire.
 * Un abonnement lie une organisation à un plan tarifaire via un processeur de paiement.
 */
@Entity
@Table(name = "subscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "pricing_plan_id", nullable = false)
    private Long pricingPlanId;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status = SubscriptionStatus.TRIALING;
    
    @Column(name = "payment_provider", nullable = false, length = 50)
    private String paymentProvider; // stripe, paypal, etc.
    
    @Column(name = "payment_provider_subscription_id", length = 255)
    private String paymentProviderSubscriptionId; // ID de l'abonnement chez le processeur
    
    @Column(name = "payment_provider_customer_id", length = 255)
    private String paymentProviderCustomerId; // ID du client chez le processeur
    
    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart;
    
    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;
    
    @Column(name = "trial_start")
    private LocalDateTime trialStart;
    
    @Column(name = "trial_end")
    private LocalDateTime trialEnd;
    
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
    
    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd = false; // Si true, l'abonnement sera annulé à la fin de la période
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SubscriptionStatus.TRIALING;
        }
        if (cancelAtPeriodEnd == null) {
            cancelAtPeriodEnd = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Statuts possibles d'un abonnement.
     */
    public enum SubscriptionStatus {
        TRIALING,       // Période d'essai active
        ACTIVE,         // Abonnement actif et payé
        PAST_DUE,       // Paiement en retard
        CANCELED,       // Abonnement annulé
        UNPAID,         // Non payé
        INCOMPLETE,     // Incomplet (paiement en attente)
        INCOMPLETE_EXPIRED, // Incomplet et expiré
        PAUSED          // En pause (si supporté)
    }
}

