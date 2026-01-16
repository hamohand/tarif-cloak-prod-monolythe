package com.muhend.backend.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour représenter un profil de marché.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketProfileDto {
    private Long id;
    private String marketVersion;
    private String countryCodeIsoAlpha2;
    private String countryCodeIsoAlpha3;
    private String countryName;
    private String countryNameNative;
    private String phonePrefix;
    private String currencyCode;
    private String currencySymbol;
    private String timezone;
    private String locale;
    private String languageCode;
    private Boolean isActive;
    private Integer displayOrder;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

