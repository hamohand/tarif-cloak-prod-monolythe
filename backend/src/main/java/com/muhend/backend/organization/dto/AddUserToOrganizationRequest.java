package com.muhend.backend.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour ajouter un utilisateur à une organisation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddUserToOrganizationRequest {
    
    @NotBlank(message = "L'ID utilisateur Keycloak est obligatoire")
    private String keycloakUserId;
}

