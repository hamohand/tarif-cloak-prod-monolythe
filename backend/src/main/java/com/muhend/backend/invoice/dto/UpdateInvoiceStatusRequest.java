package com.muhend.backend.invoice.dto;

import com.muhend.backend.invoice.model.Invoice;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la mise à jour du statut d'une facture.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvoiceStatusRequest {
    
    @NotNull(message = "Le statut est obligatoire")
    private Invoice.InvoiceStatus status;
    
    private String notes;
}

