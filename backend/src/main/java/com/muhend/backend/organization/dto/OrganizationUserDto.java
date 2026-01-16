package com.muhend.backend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les associations utilisateur-organisation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUserDto {
    
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String keycloakUserId;
    private String username; // Nom d'utilisateur depuis Keycloak
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime joinedAt;
    private Boolean enabled; // Statut d'activation du compte Keycloak
}

