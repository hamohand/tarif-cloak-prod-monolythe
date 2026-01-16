package com.muhend.backend.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer une session de checkout Stripe.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckoutSessionRequest {
    
    @NotNull(message = "L'ID du plan tarifaire est obligatoire")
    private Long pricingPlanId;
    
    private Long invoiceId; // Optionnel : pour payer une facture spécifique
    
    private String successUrl; // URL de redirection après succès (optionnel, utilise une URL par défaut si non fourni)
    
    private String cancelUrl; // URL de redirection après annulation (optionnel, utilise une URL par défaut si non fourni)
}

