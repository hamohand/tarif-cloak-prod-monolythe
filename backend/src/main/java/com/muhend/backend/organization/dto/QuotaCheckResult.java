package com.muhend.backend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résultat de la vérification du quota.
 * Indique si le quota est respecté ou dépassé, et dans ce cas, si la requête peut être facturée au prix Pay-per-Request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaCheckResult {
    /**
     * true si le quota n'est pas dépassé, false si dépassé
     */
    private boolean quotaOk;
    
    /**
     * true si le quota est dépassé mais que la requête peut être facturée au prix Pay-per-Request
     */
    private boolean canUsePayPerRequest;
    
    /**
     * Prix par requête du plan Pay-per-Request correspondant au marché (si applicable)
     */
    private java.math.BigDecimal payPerRequestPrice;
    
    /**
     * Utilisation actuelle du quota mensuel
     */
    private long currentUsage;
    
    /**
     * Quota mensuel maximum
     */
    private Integer monthlyQuota;
}








