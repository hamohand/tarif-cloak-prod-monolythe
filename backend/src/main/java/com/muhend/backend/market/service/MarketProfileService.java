package com.muhend.backend.market.service;

import com.muhend.backend.market.dto.CreateMarketProfileRequest;
import com.muhend.backend.market.dto.MarketProfileDto;
import com.muhend.backend.market.dto.UpdateMarketProfileRequest;
import com.muhend.backend.market.model.MarketProfile;
import com.muhend.backend.market.repository.MarketProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les profils de marché.
 */
@Service
@Slf4j
public class MarketProfileService {
    
    private final MarketProfileRepository marketProfileRepository;
    
    public MarketProfileService(MarketProfileRepository marketProfileRepository) {
        this.marketProfileRepository = marketProfileRepository;
    }
    
    /**
     * Récupère tous les profils de marché actifs, triés par ordre d'affichage.
     */
    @Transactional(readOnly = true)
    public List<MarketProfileDto> getAllActiveMarketProfiles() {
        List<MarketProfile> profiles = marketProfileRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return profiles.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère tous les profils de marché (actifs et inactifs), triés par ordre d'affichage.
     */
    @Transactional(readOnly = true)
    public List<MarketProfileDto> getAllMarketProfiles() {
        List<MarketProfile> profiles = marketProfileRepository.findAll();
        return profiles.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère un profil de marché par son ID.
     */
    @Transactional(readOnly = true)
    public MarketProfileDto getMarketProfileById(Long id) {
        MarketProfile profile = marketProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profil de marché introuvable avec l'ID: " + id));
        return toDto(profile);
    }
    
    /**
     * Récupère un profil de marché par sa version de marché.
     */
    @Transactional(readOnly = true)
    public MarketProfileDto getMarketProfileByVersion(String marketVersion) {
        MarketProfile profile = marketProfileRepository.findByMarketVersion(marketVersion)
                .orElseThrow(() -> new IllegalArgumentException("Profil de marché introuvable pour la version: " + marketVersion));
        return toDto(profile);
    }
    
    /**
     * Récupère un profil de marché par son code ISO alpha-2.
     */
    @Transactional(readOnly = true)
    public MarketProfileDto getMarketProfileByCountryCode(String countryCode) {
        MarketProfile profile = marketProfileRepository.findByCountryCodeIsoAlpha2(countryCode)
                .orElseThrow(() -> new IllegalArgumentException("Profil de marché introuvable pour le code pays: " + countryCode));
        return toDto(profile);
    }
    
    /**
     * Crée un nouveau profil de marché.
     */
    @Transactional
    public MarketProfileDto createMarketProfile(CreateMarketProfileRequest request) {
        // Vérifier que la version de marché n'existe pas déjà
        if (marketProfileRepository.existsByMarketVersion(request.getMarketVersion())) {
            throw new IllegalArgumentException("Un profil de marché existe déjà pour la version: " + request.getMarketVersion());
        }
        
        // Vérifier que le code ISO alpha-2 n'existe pas déjà
        if (marketProfileRepository.existsByCountryCodeIsoAlpha2(request.getCountryCodeIsoAlpha2())) {
            throw new IllegalArgumentException("Un profil de marché existe déjà pour le code pays: " + request.getCountryCodeIsoAlpha2());
        }
        
        MarketProfile profile = toEntity(request);
        profile = marketProfileRepository.save(profile);
        log.info("Profil de marché créé: {} (version: {})", profile.getCountryName(), profile.getMarketVersion());
        
        return toDto(profile);
    }
    
    /**
     * Met à jour un profil de marché existant.
     */
    @Transactional
    public MarketProfileDto updateMarketProfile(Long id, UpdateMarketProfileRequest request) {
        MarketProfile profile = marketProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profil de marché introuvable avec l'ID: " + id));
        
        // Vérifier que la version de marché n'est pas déjà utilisée par un autre profil
        if (request.getMarketVersion() != null && !request.getMarketVersion().equals(profile.getMarketVersion())) {
            if (marketProfileRepository.existsByMarketVersion(request.getMarketVersion())) {
                throw new IllegalArgumentException("Un profil de marché existe déjà pour la version: " + request.getMarketVersion());
            }
        }
        
        // Vérifier que le code ISO alpha-2 n'est pas déjà utilisé par un autre profil
        if (request.getCountryCodeIsoAlpha2() != null && !request.getCountryCodeIsoAlpha2().equals(profile.getCountryCodeIsoAlpha2())) {
            if (marketProfileRepository.existsByCountryCodeIsoAlpha2(request.getCountryCodeIsoAlpha2())) {
                throw new IllegalArgumentException("Un profil de marché existe déjà pour le code pays: " + request.getCountryCodeIsoAlpha2());
            }
        }
        
        // Mettre à jour les champs
        if (request.getMarketVersion() != null) {
            profile.setMarketVersion(request.getMarketVersion());
        }
        if (request.getCountryCodeIsoAlpha2() != null) {
            profile.setCountryCodeIsoAlpha2(request.getCountryCodeIsoAlpha2());
        }
        if (request.getCountryCodeIsoAlpha3() != null) {
            profile.setCountryCodeIsoAlpha3(request.getCountryCodeIsoAlpha3());
        }
        if (request.getCountryName() != null) {
            profile.setCountryName(request.getCountryName());
        }
        if (request.getCountryNameNative() != null) {
            profile.setCountryNameNative(request.getCountryNameNative());
        }
        if (request.getPhonePrefix() != null) {
            profile.setPhonePrefix(request.getPhonePrefix());
        }
        if (request.getCurrencyCode() != null) {
            profile.setCurrencyCode(request.getCurrencyCode());
        }
        if (request.getCurrencySymbol() != null) {
            profile.setCurrencySymbol(request.getCurrencySymbol());
        }
        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone());
        }
        if (request.getLocale() != null) {
            profile.setLocale(request.getLocale());
        }
        if (request.getLanguageCode() != null) {
            profile.setLanguageCode(request.getLanguageCode());
        }
        if (request.getIsActive() != null) {
            profile.setIsActive(request.getIsActive());
        }
        if (request.getDisplayOrder() != null) {
            profile.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getDescription() != null) {
            profile.setDescription(request.getDescription());
        }
        
        profile = marketProfileRepository.save(profile);
        log.info("Profil de marché mis à jour: {} (ID: {})", profile.getCountryName(), profile.getId());
        
        return toDto(profile);
    }
    
    /**
     * Supprime un profil de marché.
     */
    @Transactional
    public void deleteMarketProfile(Long id) {
        if (!marketProfileRepository.existsById(id)) {
            throw new IllegalArgumentException("Profil de marché introuvable avec l'ID: " + id);
        }
        marketProfileRepository.deleteById(id);
        log.info("Profil de marché supprimé (ID: {})", id);
    }
    
    /**
     * Convertit une entité MarketProfile en DTO.
     */
    private MarketProfileDto toDto(MarketProfile profile) {
        MarketProfileDto dto = new MarketProfileDto();
        dto.setId(profile.getId());
        dto.setMarketVersion(profile.getMarketVersion());
        dto.setCountryCodeIsoAlpha2(profile.getCountryCodeIsoAlpha2());
        dto.setCountryCodeIsoAlpha3(profile.getCountryCodeIsoAlpha3());
        dto.setCountryName(profile.getCountryName());
        dto.setCountryNameNative(profile.getCountryNameNative());
        dto.setPhonePrefix(profile.getPhonePrefix());
        dto.setCurrencyCode(profile.getCurrencyCode());
        dto.setCurrencySymbol(profile.getCurrencySymbol());
        dto.setTimezone(profile.getTimezone());
        dto.setLocale(profile.getLocale());
        dto.setLanguageCode(profile.getLanguageCode());
        dto.setIsActive(profile.getIsActive());
        dto.setDisplayOrder(profile.getDisplayOrder());
        dto.setDescription(profile.getDescription());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        return dto;
    }
    
    /**
     * Convertit une requête de création en entité MarketProfile.
     */
    private MarketProfile toEntity(CreateMarketProfileRequest request) {
        MarketProfile profile = new MarketProfile();
        profile.setMarketVersion(request.getMarketVersion());
        profile.setCountryCodeIsoAlpha2(request.getCountryCodeIsoAlpha2().toUpperCase());
        if (request.getCountryCodeIsoAlpha3() != null) {
            profile.setCountryCodeIsoAlpha3(request.getCountryCodeIsoAlpha3().toUpperCase());
        }
        profile.setCountryName(request.getCountryName());
        profile.setCountryNameNative(request.getCountryNameNative());
        profile.setPhonePrefix(request.getPhonePrefix());
        profile.setCurrencyCode(request.getCurrencyCode().toUpperCase());
        profile.setCurrencySymbol(request.getCurrencySymbol());
        profile.setTimezone(request.getTimezone());
        profile.setLocale(request.getLocale());
        profile.setLanguageCode(request.getLanguageCode());
        profile.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        profile.setDisplayOrder(request.getDisplayOrder());
        profile.setDescription(request.getDescription());
        return profile;
    }
}

