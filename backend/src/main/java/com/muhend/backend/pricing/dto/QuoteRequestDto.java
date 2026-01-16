package com.muhend.backend.pricing.dto;

import com.muhend.backend.pricing.model.QuoteRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les demandes de devis.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequestDto {
    
    private Long id;
    private Long organizationId;
    private String contactName;
    private String contactEmail;
    private String message;
    private QuoteRequest.QuoteStatus status;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime respondedAt;
}

