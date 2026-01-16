package com.muhend.backend.market.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant un profil de marché (pays ou région).
 * Contient toutes les informations nécessaires pour caractériser un marché.
 */
@Entity
@Table(name = "market_profile", uniqueConstraints = {
    @UniqueConstraint(columnNames = "market_version"),
    @UniqueConstraint(columnNames = "country_code_iso_alpha2")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "market_version", nullable = false, unique = true, length = 10)
    private String marketVersion; // DEFAULT, DZ, FR, etc.
    
    @Column(name = "country_code_iso_alpha2", nullable = false, unique = true, length = 2)
    private String countryCodeIsoAlpha2; // FR, DZ, US, etc. (ISO 3166-1 alpha-2)
    
    @Column(name = "country_code_iso_alpha3", length = 3)
    private String countryCodeIsoAlpha3; // FRA, DZA, USA, etc. (ISO 3166-1 alpha-3)
    
    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName; // France, Algérie, etc.
    
    @Column(name = "country_name_native", length = 100)
    private String countryNameNative; // Nom du pays dans sa langue native
    
    @Column(name = "phone_prefix", nullable = false, length = 10)
    private String phonePrefix; // +33, +213, +1, etc.
    
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode; // EUR, DZD, USD, etc. (ISO 4217)
    
    @Column(name = "currency_symbol", length = 10)
    private String currencySymbol; // €, DA, $, etc.
    
    @Column(name = "timezone", length = 50)
    private String timezone; // Europe/Paris, Africa/Algiers, etc.
    
    @Column(name = "locale", length = 10)
    private String locale; // fr_FR, ar_DZ, en_US, etc.
    
    @Column(name = "language_code", length = 5)
    private String languageCode; // fr, ar, en, etc. (ISO 639-1)
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Pour activer/désactiver un profil
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // Ordre d'affichage
    
    @Column(name = "description", length = 500)
    private String description; // Description du marché
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

