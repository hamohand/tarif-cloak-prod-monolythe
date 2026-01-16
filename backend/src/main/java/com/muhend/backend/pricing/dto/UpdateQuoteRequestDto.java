package com.muhend.backend.pricing.dto;

import com.muhend.backend.pricing.model.QuoteRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour une demande de devis (admin uniquement).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuoteRequestDto {
    
    private QuoteRequest.QuoteStatus status;
    private String adminNotes;
}

