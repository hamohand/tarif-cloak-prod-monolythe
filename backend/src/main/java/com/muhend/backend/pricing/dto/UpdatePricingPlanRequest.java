package com.muhend.backend.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour mettre à jour un plan tarifaire.
 * Tous les champs sont optionnels pour permettre une mise à jour partielle.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePricingPlanRequest {
    
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String name;
    
    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix mensuel doit être positif ou nul")
    private BigDecimal pricePerMonth; // null pour les plans facturés à la requête ou pour ne pas modifier
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix par requête doit être positif ou nul")
    private BigDecimal pricePerRequest; // null pour les plans mensuels ou pour ne pas modifier
    
    @Min(value = 0, message = "Le quota mensuel doit être positif ou nul")
    private Integer monthlyQuota; // null = quota illimité ou plan facturé à la requête ou pour ne pas modifier
    
    @Min(value = 0, message = "La période d'essai doit être positive ou nulle")
    private Integer trialPeriodDays; // null si pas un plan d'essai ou pour ne pas modifier
    
    private String features; // JSON ou texte décrivant les fonctionnalités
    
    private Boolean isActive; // Pour activer/désactiver un plan
    
    @Min(value = 0, message = "L'ordre d'affichage doit être positif ou nul")
    private Integer displayOrder; // Ordre d'affichage
}

