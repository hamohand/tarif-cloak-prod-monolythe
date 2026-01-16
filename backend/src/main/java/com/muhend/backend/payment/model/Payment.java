package com.muhend.backend.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité représentant un paiement.
 * Un paiement peut être lié à un abonnement (paiement récurrent) ou à une facture (paiement ponctuel).
 */
@Entity
@Table(name = "payment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "subscription_id")
    private Long subscriptionId; // Null pour les paiements ponctuels
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "invoice_id")
    private Long invoiceId; // Null pour les paiements d'abonnement récurrent
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Column(name = "payment_provider", nullable = false, length = 50)
    private String paymentProvider; // stripe, paypal, etc.
    
    @Column(name = "payment_provider_payment_id", length = 255)
    private String paymentProviderPaymentId; // ID du paiement chez le processeur
    
    @Column(name = "payment_provider_payment_intent_id", length = 255)
    private String paymentProviderPaymentIntentId; // ID de l'intention de paiement (Stripe)
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // card, sepa, paypal, etc.
    
    @Column(name = "payment_method_type", length = 50)
    private String paymentMethodType; // credit_card, debit_card, bank_transfer, etc.
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason; // Raison de l'échec si le paiement a échoué
    
    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl; // URL vers la facture du processeur
    
    @Column(name = "receipt_url", length = 500)
    private String receiptUrl; // URL vers le reçu du processeur
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
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
            status = PaymentStatus.PENDING;
        }
        if (currency == null) {
            currency = "EUR";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Statuts possibles d'un paiement.
     */
    public enum PaymentStatus {
        PENDING,        // En attente
        PROCESSING,    // En cours de traitement
        SUCCEEDED,     // Réussi
        FAILED,        // Échoué
        CANCELED,      // Annulé
        REFUNDED,      // Remboursé
        PARTIALLY_REFUNDED // Partiellement remboursé
    }
}

