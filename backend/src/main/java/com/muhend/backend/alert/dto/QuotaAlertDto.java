package com.muhend.backend.alert.dto;

import com.muhend.backend.alert.model.QuotaAlert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les alertes de quota.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaAlertDto {
    
    private Long id;
    private Long organizationId;
    private String organizationName;
    private QuotaAlert.AlertType alertType;
    private Long currentUsage;
    private Integer monthlyQuota;
    private Double percentageUsed;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

