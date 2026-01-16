package com.muhend.backend.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les plans tarifaires.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingPlanDto {
    
    private Long id;
    private String name;
    private String description;
    private BigDecimal pricePerMonth; // null pour les plans facturés à la requête
    private BigDecimal pricePerRequest; // null pour les plans mensuels
    private Integer monthlyQuota; // null = quota illimité ou plan facturé à la requête
    private Integer trialPeriodDays; // null si pas un plan d'essai
    private String features;
    private Boolean isActive;
    private Integer displayOrder;
    private String marketVersion;
    private String currency;
    private Boolean isCustom;
    private Long organizationId;
}

