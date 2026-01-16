package com.muhend.backend.pricing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour créer une demande de devis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuoteRequestDto {
    
    // Note: organizationId est maintenant récupéré automatiquement depuis le token dans le contrôleur
    // Il est toujours présent dans le DTO pour le service, mais n'est plus requis dans la validation
    private Long organizationId;
    
    @NotBlank(message = "Le nom du contact est requis")
    private String contactName;
    
    @NotBlank(message = "L'email du contact est requis")
    @Email(message = "L'email doit être valide")
    private String contactEmail;
    
    private String message; // Optionnel
}

