package com.muhend.backend.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour Stripe.
 * Les clés API doivent être définies dans les variables d'environnement.
 */
@Configuration
@Getter
public class StripeConfig {
    
    @Value("${stripe.secret-key:}")
    private String secretKey;
    
    @Value("${stripe.publishable-key:}")
    private String publishableKey;
    
    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;
    
    @Value("${stripe.currency:EUR}")
    private String currency;
    
    /**
     * Vérifie si Stripe est configuré.
     */
    public boolean isConfigured() {
        return secretKey != null && !secretKey.isEmpty() 
            && publishableKey != null && !publishableKey.isEmpty();
    }
}

