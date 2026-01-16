package com.muhend.backend.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer une organisation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrganizationRequest {
    
    @NotBlank(message = "Le nom de l'organisation est obligatoire")
    @Size(min = 1, max = 255, message = "Le nom doit contenir entre 1 et 255 caractères")
    private String name;
    
    @NotBlank(message = "L'email de l'organisation est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 255, message = "L'email doit contenir au maximum 255 caractères")
    private String email; // Email de l'organisation (obligatoire et identifiant unique)
    
    @NotBlank(message = "L'adresse de l'organisation est obligatoire")
    @Size(min = 5, max = 512, message = "L'adresse doit contenir entre 5 et 512 caractères")
    private String address;
    
    @Size(max = 255, message = "Le domaine d'activité doit contenir au maximum 255 caractères")
    private String activityDomain; // Domaine d'activité (optionnel)
    
    @NotBlank(message = "Le pays est obligatoire")
    @Size(min = 2, max = 2, message = "Le pays doit être un code ISO 3166-1 alpha-2")
    private String country;
    
    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Size(min = 5, max = 32, message = "Le numéro de téléphone doit contenir entre 5 et 32 caractères")
    private String phone;
    
    @NotBlank(message = "Le mot de passe de l'organisation est obligatoire")
    @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre 8 et 128 caractères")
    private String organizationPassword;
    
    private String keycloakUserId;
    
    private Long pricingPlanId; // ID du plan tarifaire sélectionné (optionnel)
    
    private String marketVersion; // Version du marché (ex: DEFAULT, DZ) - optionnel
}

