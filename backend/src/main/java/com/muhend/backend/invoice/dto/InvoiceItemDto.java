package com.muhend.backend.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les lignes de facture.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDto {
    
    private Long id;
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String itemType;
}

