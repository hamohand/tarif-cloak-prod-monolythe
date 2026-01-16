package com.muhend.backend.market.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour un profil de marché existant.
 * Tous les champs sont optionnels (seuls les champs fournis seront mis à jour).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMarketProfileRequest {
    
    @Size(max = 10, message = "La version de marché ne peut pas dépasser 10 caractères")
    private String marketVersion;
    
    @Size(min = 2, max = 2, message = "Le code ISO alpha-2 doit contenir exactement 2 caractères")
    private String countryCodeIsoAlpha2;
    
    @Size(max = 3, message = "Le code ISO alpha-3 ne peut pas dépasser 3 caractères")
    private String countryCodeIsoAlpha3;
    
    @Size(max = 100, message = "Le nom du pays ne peut pas dépasser 100 caractères")
    private String countryName;
    
    @Size(max = 100, message = "Le nom natif du pays ne peut pas dépasser 100 caractères")
    private String countryNameNative;
    
    @Size(max = 10, message = "L'indicatif téléphonique ne peut pas dépasser 10 caractères")
    private String phonePrefix;
    
    @Size(min = 3, max = 3, message = "Le code devise doit contenir exactement 3 caractères")
    private String currencyCode;
    
    @Size(max = 10, message = "Le symbole de devise ne peut pas dépasser 10 caractères")
    private String currencySymbol;
    
    @Size(max = 50, message = "Le fuseau horaire ne peut pas dépasser 50 caractères")
    private String timezone;
    
    @Size(max = 10, message = "La locale ne peut pas dépasser 10 caractères")
    private String locale;
    
    @Size(max = 5, message = "Le code langue ne peut pas dépasser 5 caractères")
    private String languageCode;
    
    private Boolean isActive;
    
    private Integer displayOrder;
    
    @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
    private String description;
}

