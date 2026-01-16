package com.muhend.backend.market.controller;

import com.muhend.backend.market.dto.CreateMarketProfileRequest;
import com.muhend.backend.market.dto.MarketProfileDto;
import com.muhend.backend.market.dto.UpdateMarketProfileRequest;
import com.muhend.backend.market.service.MarketProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Controller REST pour gérer les profils de marché.
 */
@RestController
@RequestMapping("/market-profiles")
@Tag(name = "Market Profiles", description = "Gestion des profils de marché (pays/régions)")
@Slf4j
public class MarketProfileController {
    
    private final MarketProfileService marketProfileService;
    
    public MarketProfileController(MarketProfileService marketProfileService) {
        this.marketProfileService = marketProfileService;
    }
    
    /**
     * Récupère tous les profils de marché actifs.
     */
    @GetMapping
    @Operation(
        summary = "Récupérer tous les profils de marché actifs",
        description = "Retourne la liste de tous les profils de marché actifs, triés par ordre d'affichage."
    )
    public ResponseEntity<List<MarketProfileDto>> getAllActiveMarketProfiles() {
        List<MarketProfileDto> profiles = marketProfileService.getAllActiveMarketProfiles();
        return ResponseEntity.ok(profiles);
    }
    
    /**
     * Récupère tous les profils de marché (actifs et inactifs).
     * Nécessite le rôle ADMIN.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Récupérer tous les profils de marché",
        description = "Retourne la liste de tous les profils de marché (actifs et inactifs). Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<MarketProfileDto>> getAllMarketProfiles() {
        List<MarketProfileDto> profiles = marketProfileService.getAllMarketProfiles();
        return ResponseEntity.ok(profiles);
    }
    
    /**
     * Récupère un profil de marché par son ID.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Récupérer un profil de marché par ID",
        description = "Retourne les détails d'un profil de marché spécifique."
    )
    public ResponseEntity<MarketProfileDto> getMarketProfileById(@PathVariable Long id) {
        MarketProfileDto profile = marketProfileService.getMarketProfileById(id);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Récupère un profil de marché par sa version de marché.
     */
    @GetMapping("/version/{marketVersion}")
    @Operation(
        summary = "Récupérer un profil de marché par version",
        description = "Retourne les détails d'un profil de marché à partir de sa version (ex: DEFAULT, DZ)."
    )
    public ResponseEntity<MarketProfileDto> getMarketProfileByVersion(@PathVariable String marketVersion) {
        MarketProfileDto profile = marketProfileService.getMarketProfileByVersion(marketVersion);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Récupère un profil de marché par son code ISO alpha-2.
     */
    @GetMapping("/country/{countryCode}")
    @Operation(
        summary = "Récupérer un profil de marché par code pays",
        description = "Retourne les détails d'un profil de marché à partir du code ISO alpha-2 (ex: FR, DZ)."
    )
    public ResponseEntity<MarketProfileDto> getMarketProfileByCountryCode(@PathVariable String countryCode) {
        MarketProfileDto profile = marketProfileService.getMarketProfileByCountryCode(countryCode.toUpperCase());
        return ResponseEntity.ok(profile);
    }
    
    /**
     * Crée un nouveau profil de marché.
     * Nécessite le rôle ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Créer un nouveau profil de marché",
        description = "Crée un nouveau profil de marché. Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MarketProfileDto> createMarketProfile(@Valid @RequestBody CreateMarketProfileRequest request) {
        MarketProfileDto created = marketProfileService.createMarketProfile(request);
        return ResponseEntity.ok(created);
    }
    
    /**
     * Met à jour un profil de marché existant.
     * Nécessite le rôle ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Mettre à jour un profil de marché",
        description = "Met à jour les informations d'un profil de marché existant. Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<MarketProfileDto> updateMarketProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMarketProfileRequest request) {
        MarketProfileDto updated = marketProfileService.updateMarketProfile(id, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Supprime un profil de marché.
     * Nécessite le rôle ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Supprimer un profil de marché",
        description = "Supprime un profil de marché. Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> deleteMarketProfile(@PathVariable Long id) {
        marketProfileService.deleteMarketProfile(id);
        return ResponseEntity.ok(Map.of("message", "Profil de marché supprimé avec succès"));
    }
}

