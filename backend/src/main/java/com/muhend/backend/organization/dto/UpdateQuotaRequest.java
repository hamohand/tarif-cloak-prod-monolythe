package com.muhend.backend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour le quota mensuel d'une organisation.
 * Phase 4 MVP : Quotas Basiques
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuotaRequest {
    
    /**
     * Nouveau quota mensuel (null pour quota illimité).
     */
    private Integer monthlyQuota;
}

