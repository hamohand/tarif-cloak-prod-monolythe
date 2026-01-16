package com.muhend.backend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour changer le plan tarifaire d'une organisation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePricingPlanRequest {
    private Long pricingPlanId; // null pour retirer le plan tarifaire
}

