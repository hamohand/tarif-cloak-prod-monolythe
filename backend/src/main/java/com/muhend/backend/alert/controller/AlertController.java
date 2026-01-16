package com.muhend.backend.alert.controller;

import com.muhend.backend.alert.dto.QuotaAlertDto;
import com.muhend.backend.alert.service.QuotaAlertService;
import com.muhend.backend.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour gérer les alertes de quota.
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Alerts", description = "Gestion des alertes de quota")
public class AlertController {
    
    private final QuotaAlertService quotaAlertService;
    private final OrganizationService organizationService;
    
    /**
     * Récupère les alertes non lues de l'utilisateur connecté (basées sur son organisation).
     * 
     * IMPORTANT : Les alertes sont relatives à la consommation de l'organisation,
     * pas à la consommation personnelle de l'utilisateur.
     * Tous les collaborateurs d'une organisation voient les mêmes alertes.
     */
    @GetMapping("/my-alerts")
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZATION', 'COLLABORATOR', 'ADMIN')")
    @Operation(
        summary = "Récupérer mes alertes",
        description = "Retourne les alertes non lues de l'organisation de l'utilisateur connecté. " +
                     "Les alertes sont basées sur la consommation totale de l'organisation (somme de tous les collaborateurs).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<QuotaAlertDto>> getMyAlerts() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.ok(List.of()); // Pas d'organisation, pas d'alertes
        }
        
        List<QuotaAlertDto> alerts = quotaAlertService.getUnreadAlertsForOrganization(organizationId);
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Compte les alertes non lues de l'utilisateur connecté.
     * 
     * IMPORTANT : Les alertes sont relatives à la consommation de l'organisation,
     * pas à la consommation personnelle de l'utilisateur.
     */
    @GetMapping("/my-alerts/count")
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZATION', 'COLLABORATOR', 'ADMIN')")
    @Operation(
        summary = "Compter mes alertes non lues",
        description = "Retourne le nombre d'alertes non lues de l'organisation de l'utilisateur connecté. " +
                     "Les alertes sont basées sur la consommation totale de l'organisation (somme de tous les collaborateurs).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> getMyAlertsCount() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.ok(Map.of("count", 0));
        }
        
        long count = quotaAlertService.countUnreadAlertsForOrganization(organizationId);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    /**
     * Récupère toutes les alertes non lues (pour les admins).
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Récupérer toutes les alertes non lues",
        description = "Retourne toutes les alertes non lues de toutes les organisations. Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<QuotaAlertDto>> getAllAlerts() {
        List<QuotaAlertDto> alerts = quotaAlertService.getAllUnreadAlerts();
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Compte toutes les alertes non lues (pour les admins).
     */
    @GetMapping("/admin/count")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Compter toutes les alertes non lues",
        description = "Retourne le nombre total d'alertes non lues. Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> getAllAlertsCount() {
        long count = quotaAlertService.countAllUnreadAlerts();
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    /**
     * Récupère les alertes d'une organisation spécifique (pour les admins).
     */
    @GetMapping("/admin/organization/{organizationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Récupérer les alertes d'une organisation",
        description = "Retourne toutes les alertes d'une organisation. Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<QuotaAlertDto>> getAlertsForOrganization(@PathVariable Long organizationId) {
        List<QuotaAlertDto> alerts = quotaAlertService.getAllAlertsForOrganization(organizationId);
        return ResponseEntity.ok(alerts);
    }
    
    /**
     * Marque une alerte comme lue.
     */
    @PutMapping("/{alertId}/read")
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZATION', 'COLLABORATOR', 'ADMIN')")
    @Operation(
        summary = "Marquer une alerte comme lue",
        description = "Marque une alerte spécifique comme lue.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> markAlertAsRead(@PathVariable Long alertId) {
        quotaAlertService.markAlertAsRead(alertId);
        return ResponseEntity.ok(Map.of("message", "Alerte marquée comme lue"));
    }
    
    /**
     * Marque toutes les alertes de l'organisation de l'utilisateur comme lues.
     */
    @PutMapping("/my-alerts/read-all")
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZATION', 'COLLABORATOR', 'ADMIN')")
    @Operation(
        summary = "Marquer toutes mes alertes comme lues",
        description = "Marque toutes les alertes de l'organisation de l'utilisateur connecté comme lues.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> markAllMyAlertsAsRead() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.ok(Map.of("message", "Aucune organisation associée"));
        }
        
        quotaAlertService.markAllAlertsAsReadForOrganization(organizationId);
        return ResponseEntity.ok(Map.of("message", "Toutes les alertes marquées comme lues"));
    }
    
    /**
     * Déclenche manuellement une vérification des quotas pour toutes les organisations (admin uniquement).
     */
    @PostMapping("/admin/check-quotas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Vérifier manuellement les quotas",
        description = "Déclenche une vérification manuelle des quotas pour toutes les organisations et crée des alertes si nécessaire. Nécessite le rôle ADMIN.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> checkQuotasManually() {
        quotaAlertService.checkAllOrganizations();
        return ResponseEntity.ok(Map.of("message", "Vérification des quotas effectuée"));
    }
    
    /**
     * Récupère l'ID de l'utilisateur Keycloak depuis le contexte de sécurité.
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                return jwt.getClaimAsString("sub");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ID utilisateur", e);
        }
        return null;
    }
}

