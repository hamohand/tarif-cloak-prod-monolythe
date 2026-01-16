package com.muhend.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse d'une session de checkout.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionResponse {
    
    private String sessionId; // ID de la session Stripe
    private String url; // URL de redirection vers Stripe Checkout
    private String publishableKey; // Clé publique Stripe (pour le frontend)
}

