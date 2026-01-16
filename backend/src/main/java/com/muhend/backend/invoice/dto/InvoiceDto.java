package com.muhend.backend.invoice.dto;

import com.muhend.backend.invoice.model.Invoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour les factures.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {
    
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String organizationEmail;
    private String invoiceNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalAmount;
    private Invoice.InvoiceStatus status;
    private LocalDateTime createdAt;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private String notes;
    private LocalDateTime viewedAt;
    private List<InvoiceItemDto> items;
    
    // Statistiques d'utilisation pour la période
    private Long totalRequests;
    private Long totalTokens;
    private BigDecimal totalCostUsd;
}

