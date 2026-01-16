package com.muhend.backend.pricing.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité représentant un plan tarifaire.
 */
@Entity
@Table(name = "pricing_plan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicInsert
@DynamicUpdate
public class PricingPlan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name; // Ex: "Starter", "Professional", "Enterprise"
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "price_per_month", columnDefinition = "NUMERIC(10,2)")
    private BigDecimal pricePerMonth; // Prix mensuel en EUR (null pour les plans facturés à la requête)
    
    @Column(name = "price_per_request", columnDefinition = "NUMERIC(10,2)")
    private BigDecimal pricePerRequest; // Prix par requête en EUR (null pour les plans mensuels)
    
    @Column(name = "monthly_quota", nullable = true)
    private Integer monthlyQuota; // null = quota illimité ou plan facturé à la requête
    
    @Column(name = "trial_period_days", nullable = true)
    private Integer trialPeriodDays; // Nombre de jours de période d'essai (null si pas un plan d'essai)
    
    @Column(name = "features", columnDefinition = "TEXT")
    private String features; // JSON ou texte décrivant les fonctionnalités
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Pour activer/désactiver un plan
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // Ordre d'affichage
    
    @Column(name = "market_version", length = 10)
    private String marketVersion = "DEFAULT"; // DEFAULT, DZ, etc.
    
    @Column(name = "currency", length = 3)
    private String currency = "EUR"; // EUR, DZD, etc.
    
    @Column(name = "is_custom", nullable = false)
    private Boolean isCustom = false; // true pour les plans créés via devis
    
    @Column(name = "organization_id", nullable = true)
    private Long organizationId; // Pour les plans personnalisés par organisation
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

