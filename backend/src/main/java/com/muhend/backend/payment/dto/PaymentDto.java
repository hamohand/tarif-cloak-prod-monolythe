package com.muhend.backend.payment.dto;

import com.muhend.backend.payment.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les paiements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    
    private Long id;
    private Long subscriptionId;
    private Long organizationId;
    private Long invoiceId;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private String paymentProvider;
    private String paymentProviderPaymentId;
    private String paymentProviderPaymentIntentId;
    private String paymentMethod;
    private String paymentMethodType;
    private String description;
    private String failureReason;
    private String invoiceUrl;
    private String receiptUrl;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

