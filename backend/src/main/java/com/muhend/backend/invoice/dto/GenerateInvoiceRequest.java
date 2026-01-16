package com.muhend.backend.invoice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO pour la requête de génération de facture.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateInvoiceRequest {
    
    @NotNull(message = "L'ID de l'organisation est obligatoire")
    private Long organizationId;
    
    @NotNull(message = "La date de début de la période est obligatoire")
    private LocalDate periodStart;
    
    @NotNull(message = "La date de fin de la période est obligatoire")
    private LocalDate periodEnd;
}

