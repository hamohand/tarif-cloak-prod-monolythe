package com.muhend.backend.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour une organisation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationRequest {
    
    @Size(min = 1, max = 255, message = "Le nom doit contenir entre 1 et 255 caractères")
    private String name;
    
    @Email(message = "Format d'email invalide")
    @Size(max = 255, message = "L'email doit contenir au maximum 255 caractères")
    private String email; // Email de contact (optionnel)
    
    @Size(min = 5, max = 512, message = "L'adresse doit contenir entre 5 et 512 caractères")
    private String address;
    
    @Size(max = 255, message = "Le domaine d'activité doit contenir au maximum 255 caractères")
    private String activityDomain; // Domaine d'activité (optionnel)
    
    @Size(min = 2, max = 2, message = "Le pays doit être un code ISO 3166-1 alpha-2")
    private String country;
    
    @Size(min = 5, max = 32, message = "Le numéro de téléphone doit contenir entre 5 et 32 caractères")
    private String phone;
}

