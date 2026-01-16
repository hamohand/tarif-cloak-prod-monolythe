package com.muhend.backend.alert.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant une alerte de quota.
 * Les alertes sont créées automatiquement quand un quota approche ou dépasse sa limite.
 */
@Entity
@Table(name = "quota_alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotaAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
    
    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName;
    
    @Column(name = "alert_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AlertType alertType; // WARNING, CRITICAL, EXCEEDED
    
    @Column(name = "current_usage", nullable = false)
    private Long currentUsage;
    
    @Column(name = "monthly_quota", nullable = true)
    private Integer monthlyQuota;
    
    @Column(name = "percentage_used", nullable = false)
    private Double percentageUsed;
    
    @Column(name = "message", nullable = false, length = 500)
    private String message;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
    }
    
    public enum AlertType {
        WARNING,    // 80-99% utilisé
        CRITICAL,   // 100% utilisé (quota atteint)
        EXCEEDED    // > 100% utilisé (quota dépassé)
    }
}

