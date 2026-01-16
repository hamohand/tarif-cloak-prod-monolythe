package com.muhend.backend.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant une inscription en attente de confirmation par email.
 */
@Entity
@Table(name = "pending_registration", 
       indexes = {
           @Index(name = "idx_token", columnList = "confirmation_token"),
           @Index(name = "idx_email", columnList = "organization_email"),
           @Index(name = "idx_expires_at", columnList = "expires_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, length = 255)
    private String username;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "first_name", length = 255)
    private String firstName;
    
    @Column(name = "last_name", length = 255)
    private String lastName;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password; // Hashé ou en clair selon votre politique de sécurité
    
    @Column(name = "organization_password", nullable = true, length = 255)
    private String organizationPassword;
    
    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName;
    
    @Column(name = "organization_email", nullable = false, length = 255)
    private String organizationEmail;
    
    @Column(name = "organization_address", nullable = false, length = 512)
    private String organizationAddress;
    
    @Column(name = "organization_activity_domain", nullable = true, length = 255)
    private String organizationActivityDomain; // Domaine d'activité (optionnel)
    
    @Column(name = "organization_country", nullable = false, length = 2)
    private String organizationCountry;
    
    @Column(name = "organization_phone", nullable = false, length = 32)
    private String organizationPhone;
    
    @Column(name = "pricing_plan_id", nullable = true)
    private Long pricingPlanId; // ID du plan tarifaire sélectionné (optionnel)
    
    @Column(name = "market_version", length = 10)
    private String marketVersion; // Version du marché (ex: DEFAULT, DZ) - optionnel
    
    @Column(name = "confirmation_token", nullable = false, unique = true, length = 64)
    private String confirmationToken;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "confirmed", nullable = false)
    private Boolean confirmed = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (confirmed == null) {
            confirmed = false;
        }
    }
}

