package com.muhend.backend.usage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité pour enregistrer les logs d'utilisation des recherches de codes.
 * Phase 1 MVP : Tracking basique des recherches.
 */
@Entity
@Table(name = "usage_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;
    
    @Column(name = "organization_id")
    private Long organizationId;  // ID de l'organisation (peut être null si l'utilisateur n'a pas d'organisation)
    
    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;  // "/recherche/sections", "/recherche/chapitres", etc.
    
    @Column(name = "search_term", length = 500)
    private String searchTerm;
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "cost_usd", precision = 10, scale = 6)
    private BigDecimal costUsd;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

