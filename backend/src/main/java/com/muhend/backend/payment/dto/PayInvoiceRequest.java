package com.muhend.backend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour payer une facture.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayInvoiceRequest {
    
    private String successUrl; // URL de redirection après succès (optionnel)
    
    private String cancelUrl; // URL de redirection après annulation (optionnel)
}

