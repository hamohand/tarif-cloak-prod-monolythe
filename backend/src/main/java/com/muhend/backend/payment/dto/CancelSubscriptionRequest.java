package com.muhend.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour annuler un abonnement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelSubscriptionRequest {
    
    /**
     * Si true, l'abonnement sera annulé à la fin de la période actuelle.
     * Si false, l'abonnement sera annulé immédiatement.
     */
    private Boolean cancelAtPeriodEnd = true;
}

