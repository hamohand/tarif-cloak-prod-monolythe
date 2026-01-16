package com.muhend.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationRequest {
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    private String firstName;

    private String lastName;
    
    // Informations de l'organisation
    @NotBlank(message = "Le nom de l'organisation est obligatoire")
    @Size(min = 1, max = 255, message = "Le nom de l'organisation doit contenir entre 1 et 255 caractères")
    private String organizationName;
    
    @NotBlank(message = "L'email de l'organisation est obligatoire")
    @Email(message = "Format d'email invalide pour l'organisation")
    @Size(max = 255, message = "L'email de l'organisation doit contenir au maximum 255 caractères")
    private String organizationEmail; // Obligatoire et utilisé comme identifiant unique
    
    @NotBlank(message = "L'adresse de l'organisation est obligatoire")
    @Size(min = 5, max = 512, message = "L'adresse doit contenir entre 5 et 512 caractères")
    private String organizationAddress;
    
    @Size(max = 255, message = "Le domaine d'activité doit contenir au maximum 255 caractères")
    private String organizationActivityDomain; // Domaine d'activité (optionnel)
    
    @NotBlank(message = "Le pays de l'organisation est obligatoire")
    @Size(min = 2, max = 2, message = "Le pays doit être un code ISO 3166-1 alpha-2")
    private String organizationCountry;
    
    @NotBlank(message = "Le numéro de téléphone de l'organisation est obligatoire")
    @Size(min = 5, max = 32, message = "Le numéro de téléphone doit contenir entre 5 et 32 caractères")
    private String organizationPhone;
    
    @NotBlank(message = "Le mot de passe de l'organisation est obligatoire")
    @Size(min = 8, max = 128, message = "Le mot de passe de l'organisation doit contenir entre 8 et 128 caractères")
    private String organizationPassword;
    
    private Long pricingPlanId; // ID du plan tarifaire sélectionné (optionnel)
    
    private String marketVersion; // Version du marché (ex: DEFAULT, DZ) - optionnel, utilisé pour pré-remplir les champs
}
