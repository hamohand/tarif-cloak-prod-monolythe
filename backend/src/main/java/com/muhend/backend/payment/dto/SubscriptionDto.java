package com.muhend.backend.payment.dto;

import com.muhend.backend.payment.model.Subscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les abonnements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto {
    
    private Long id;
    private Long organizationId;
    private Long pricingPlanId;
    private Subscription.SubscriptionStatus status;
    private String paymentProvider;
    private String paymentProviderSubscriptionId;
    private String paymentProviderCustomerId;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private LocalDateTime canceledAt;
    private Boolean cancelAtPeriodEnd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

