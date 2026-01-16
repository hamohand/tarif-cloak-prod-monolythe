package com.muhend.backend.invoice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant une facture.
 * Une facture est générée mensuellement pour une organisation basée sur son utilisation.
 */
@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName;
    
    @Column(name = "organization_email", length = 255)
    private String organizationEmail;
    
    @Column(name = "invoice_number", unique = true, nullable = false, length = 50)
    private String invoiceNumber;
    
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
    
    // Colonnes pour l'intégration avec le système de paiement
    @Column(name = "payment_id")
    private Long paymentId; // Référence au paiement associé
    
    @Column(name = "subscription_id")
    private Long subscriptionId; // Référence à l'abonnement associé (si applicable)
    
    @Column(name = "payment_provider", length = 50)
    private String paymentProvider; // stripe, paypal, etc.
    
    @Column(name = "payment_provider_invoice_id", length = 255)
    private String paymentProviderInvoiceId; // ID de la facture chez le processeur
    
    @Column(name = "payment_intent_id", length = 255)
    private String paymentIntentId; // ID de l'intention de paiement (Stripe)
    
    @Column(name = "invoice_pdf_url", length = 500)
    private String invoicePdfUrl; // URL vers le PDF (si stocké ailleurs)
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = InvoiceStatus.PENDING;
        }
        if (dueDate == null && periodEnd != null) {
            // La date d'échéance est généralement 30 jours après la fin de la période
            dueDate = periodEnd.plusDays(30);
        }
    }
    
    public enum InvoiceStatus {
        DRAFT,      // Brouillon
        PENDING,    // En attente de paiement
        PAID,       // Payé
        OVERDUE,    // En retard
        CANCELLED   // Annulé
    }
}

