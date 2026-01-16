package com.muhend.backend.organization.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité de liaison entre un utilisateur Keycloak et une organisation.
 * Phase 2 MVP : Association Utilisateur → Entreprise
 */
@Entity
@Table(name = "organization_user", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "keycloak_user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Column(name = "keycloak_user_id", nullable = false, length = 255)
    private String keycloakUserId;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
    
    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}

