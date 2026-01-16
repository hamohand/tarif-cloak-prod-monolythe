package com.muhend.backend.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCollaboratorRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 255, message = "Le nom d'utilisateur doit contenir entre 3 et 255 caractères")
    private String username;

    @NotBlank(message = "L'email utilisateur est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 255, message = "L'email doit contenir au maximum 255 caractères")
    private String email;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 1, max = 255, message = "Le prénom doit contenir entre 1 et 255 caractères")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 1, max = 255, message = "Le nom doit contenir entre 1 et 255 caractères")
    private String lastName;
}

