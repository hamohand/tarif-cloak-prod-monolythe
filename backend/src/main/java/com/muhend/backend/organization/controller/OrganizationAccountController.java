package com.muhend.backend.organization.controller;

import com.muhend.backend.auth.model.PendingRegistration;
import com.muhend.backend.auth.service.KeycloakAdminService;
import com.muhend.backend.auth.service.PendingRegistrationService;
import com.muhend.backend.organization.dto.CreateCollaboratorRequest;
import com.muhend.backend.organization.dto.OrganizationDto;
import com.muhend.backend.organization.dto.OrganizationUserDto;
import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.usage.model.UsageLog;
import com.muhend.backend.usage.repository.UsageLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.keycloak.representations.idm.UserRepresentation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/organization/account")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Account", description = "Endpoints pour les comptes organisations (gestion des collaborateurs)")
public class OrganizationAccountController {

    private final PendingRegistrationService pendingRegistrationService;
    private final OrganizationService organizationService;
    private final UsageLogRepository usageLogRepository;
    private final KeycloakAdminService keycloakAdminService;
    
    @Value("${BASE_REQUEST_PRICE:0.01}")
    private String baseRequestPriceStr;
    
    private double baseRequestPrice;
    
    @jakarta.annotation.PostConstruct
    private void initBaseRequestPrice() {
        try {
            log.info("Valeur brute de BASE_REQUEST_PRICE reçue: '{}'", baseRequestPriceStr);
            // Nettoyer la valeur pour éviter les problèmes de concaténation dans le fichier .env
            String cleaned = baseRequestPriceStr != null ? baseRequestPriceStr.trim() : "0.01";
            // Extraire seulement la partie numérique (avant tout caractère non numérique ou espace)
            cleaned = cleaned.split("\\s+")[0]; // Prendre le premier mot
            cleaned = cleaned.replaceAll("[^0-9.]", ""); // Enlever tout sauf chiffres et point
            if (cleaned.isEmpty() || cleaned.equals(".")) {
                cleaned = "0.01";
                log.warn("Valeur nettoyée vide ou invalide, utilisation de la valeur par défaut: {}", cleaned);
            }
            baseRequestPrice = Double.parseDouble(cleaned);
            log.info("✅ Tarif de base par requête configuré avec succès: {} (dans la devise du marché, valeur originale: '{}')", baseRequestPrice, baseRequestPriceStr);
        } catch (NumberFormatException e) {
            log.error("❌ Erreur lors du parsing de BASE_REQUEST_PRICE: '{}'. Utilisation de la valeur par défaut 0.01", baseRequestPriceStr, e);
            baseRequestPrice = 0.01;
        }
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "État de l'organisation",
            description = "Retourne l'état de l'organisation (essai expiré, peut faire des requêtes, etc.).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getOrganizationStatus() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            if (organizationId == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "ORGANIZATION_NOT_FOUND", "message", "Organisation introuvable"));
            }
            
            // Vérifier si l'organisation peut faire des requêtes (cela met à jour trialPermanentlyExpired si nécessaire)
            // Cette méthode appelle isTrialExpired() qui met à jour trialPermanentlyExpired dans la base
            boolean canMakeRequests = organizationService.canOrganizationMakeRequests(organizationId);
            boolean isTrialExpired = !canMakeRequests;
            
            // Récupérer l'organisation APRÈS la vérification pour avoir la valeur mise à jour de trialPermanentlyExpired
            OrganizationDto organization = organizationService.getOrganizationById(organizationId);
            boolean trialPermanentlyExpired = Boolean.TRUE.equals(organization.getTrialPermanentlyExpired());
            
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("canMakeRequests", canMakeRequests);
            status.put("isTrialExpired", isTrialExpired);
            status.put("trialPermanentlyExpired", trialPermanentlyExpired);
            status.put("trialExpiresAt", organization.getTrialExpiresAt());
            status.put("hasPricingPlan", organization.getPricingPlanId() != null);
            
            // Si l'essai est définitivement terminé, cela signifie que le quota a été atteint
            if (trialPermanentlyExpired) {
                status.put("message", "Le quota de l'essai gratuit a été atteint et est maintenant définitivement désactivé. Aucune requête HS-code n'est autorisée.");
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'état de l'organisation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Erreur lors de la récupération de l'état"));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Mon organisation",
            description = "Retourne les informations de l'organisation liée au compte connecté.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getMyOrganization() {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            return ResponseEntity.ok(organization);
        } catch (IllegalArgumentException e) {
            log.warn("Organisation introuvable pour l'utilisateur {}", organizationUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "ORGANIZATION_NOT_FOUND", "message", e.getMessage()));
        }
    }

    @GetMapping("/collaborators")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Lister les collaborateurs",
            description = "Retourne la liste des collaborateurs de l'organisation liée au compte connecté.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getMyCollaborators() {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            java.util.List<OrganizationUserDto> collaborators =
                    organizationService.getOrganizationUsersByKeycloakUserId(organizationUserId);
            return ResponseEntity.ok(Map.of(
                    "organization", organization,
                    "collaborators", collaborators
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Impossible de lister les collaborateurs pour {}: {}", organizationUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "ORGANIZATION_NOT_FOUND", "message", e.getMessage()));
        }
    }

    @PostMapping("/collaborators")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Inviter un collaborateur",
            description = "Crée une invitation pour un collaborateur. Un email de confirmation est envoyé au collaborateur pour valider son compte.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> inviteCollaborator(
            @Valid @RequestBody CreateCollaboratorRequest request) {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            PendingRegistration pendingRegistration = pendingRegistrationService.inviteCollaborator(organizationUserId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Invitation envoyée. Le collaborateur doit confirmer son compte via l'email reçu.",
                    "tokenExpiresAt", pendingRegistration.getExpiresAt()
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur validation invitation collaborateur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "INVITATION_INVALID", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Erreur lors de l'invitation d'un collaborateur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INVITATION_ERROR", "message", e.getMessage()));
        }
    }

    @PutMapping("/collaborators/{keycloakUserId}/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Désactiver un collaborateur",
            description = "Désactive le compte Keycloak d'un collaborateur de l'organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> disableCollaborator(@PathVariable String keycloakUserId) {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            organizationService.disableCollaborator(organization.getId(), keycloakUserId);
            return ResponseEntity.ok(Map.of("message", "Collaborateur désactivé avec succès"));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur lors de la désactivation du collaborateur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "DISABLE_ERROR", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Erreur lors de la désactivation d'un collaborateur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "DISABLE_ERROR", "message", e.getMessage()));
        }
    }

    @PutMapping("/collaborators/{keycloakUserId}/enable")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Activer un collaborateur",
            description = "Active le compte Keycloak d'un collaborateur de l'organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> enableCollaborator(@PathVariable String keycloakUserId) {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            organizationService.enableCollaborator(organization.getId(), keycloakUserId);
            return ResponseEntity.ok(Map.of("message", "Collaborateur activé avec succès"));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur lors de l'activation du collaborateur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "ENABLE_ERROR", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Erreur lors de l'activation d'un collaborateur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "ENABLE_ERROR", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/collaborators/{keycloakUserId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Supprimer un collaborateur",
            description = "Retire un collaborateur de l'organisation et supprime son compte Keycloak.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> deleteCollaborator(@PathVariable String keycloakUserId) {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            organizationService.deleteCollaborator(organization.getId(), keycloakUserId);
            return ResponseEntity.ok(Map.of("message", "Collaborateur supprimé avec succès"));
        } catch (IllegalArgumentException e) {
            log.warn("Erreur lors de la suppression du collaborateur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "DELETE_ERROR", "message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Erreur lors de la suppression d'un collaborateur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "DELETE_ERROR", "message", e.getMessage()));
        }
    }

    @GetMapping("/usage-logs")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer toutes les requêtes de l'organisation",
            description = "Retourne la liste de toutes les requêtes effectuées par les collaborateurs de l'organisation, " +
                         "avec les noms des collaborateurs. Paramètres optionnels: ?startDate=... et ?endDate=... pour filtrer par période (format: yyyy-MM-dd).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getOrganizationUsageLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            
            // Déterminer la période
            LocalDateTime startDateTime;
            LocalDateTime endDateTime;
            
            if (startDate != null && endDate != null) {
                startDateTime = startDate.atStartOfDay();
                endDateTime = endDate.atTime(LocalTime.MAX);
            } else {
                // Par défaut, ce mois en cours
                LocalDateTime now = LocalDateTime.now();
                startDateTime = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endDateTime = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                        .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            }
            
            // Récupérer tous les logs de l'organisation
            List<UsageLog> logs = usageLogRepository.findByOrganizationIdAndTimestampBetween(
                    organization.getId(), startDateTime, endDateTime);
            
            // Récupérer les informations des collaborateurs depuis Keycloak
            Map<String, String> userNamesMap = new LinkedHashMap<>();
            for (UsageLog usageLog : logs) {
                String keycloakUserId = usageLog.getKeycloakUserId();
                if (!userNamesMap.containsKey(keycloakUserId)) {
                    try {
                        UserRepresentation user = keycloakAdminService.getUserRepresentation(keycloakUserId);
                        if (user != null) {
                            String firstName = user.getFirstName();
                            String lastName = user.getLastName();
                            String username = user.getUsername();
                            String fullName = (firstName != null && lastName != null) 
                                    ? firstName + " " + lastName 
                                    : (firstName != null ? firstName : (lastName != null ? lastName : username));
                            userNamesMap.put(keycloakUserId, fullName);
                        } else {
                            userNamesMap.put(keycloakUserId, "Utilisateur inconnu");
                        }
                    } catch (Exception e) {
                        log.warn("Impossible de récupérer les informations de l'utilisateur {}: {}", keycloakUserId, e.getMessage());
                        userNamesMap.put(keycloakUserId, "Utilisateur inconnu");
                    }
                }
            }
            
            // Convertir les logs en Map avec les noms des collaborateurs
            List<Map<String, Object>> usageLogsWithNames = logs.stream()
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .map(usageLog -> {
                        Map<String, Object> logMap = new LinkedHashMap<>();
                        logMap.put("id", usageLog.getId());
                        logMap.put("keycloakUserId", usageLog.getKeycloakUserId());
                        logMap.put("collaboratorName", userNamesMap.getOrDefault(usageLog.getKeycloakUserId(), "Utilisateur inconnu"));
                        logMap.put("endpoint", usageLog.getEndpoint());
                        logMap.put("searchTerm", usageLog.getSearchTerm());
                        logMap.put("tokensUsed", usageLog.getTokensUsed());
                        
                        // NOUVELLE POLITIQUE : 
                        // - costUsd = BASE_REQUEST_PRICE (dans la devise du marché) = prix de la requête
                        // - tokenCostUsd = coût des tokens en USD (affiché uniquement aux admins)
                        BigDecimal requestPrice = usageLog.getCostUsd() != null ? usageLog.getCostUsd() : BigDecimal.ZERO;
                        BigDecimal baseCost = BigDecimal.valueOf(baseRequestPrice);
                        
                        // Calculer le coût des tokens en USD à partir de tokensUsed
                        // Utiliser les mêmes tarifs que dans OpenAiService
                        BigDecimal tokenCost = BigDecimal.ZERO;
                        if (usageLog.getTokensUsed() != null && usageLog.getTokensUsed() > 0) {
                            // Tarifs GPT-4o mini (au 1er sept 2025) - en USD
                            final double PRICE_INPUT_USD = 0.15 / 1_000_000;   // $ par token input
                            final double PRICE_OUTPUT_USD = 0.60 / 1_000_000;  // $ par token output
                            
                            // Estimation : on suppose un ratio moyen de 70% input / 30% output
                            // (basé sur les observations typiques des requêtes)
                            int totalTokens = usageLog.getTokensUsed();
                            int estimatedPromptTokens = (int) (totalTokens * 0.7);
                            int estimatedCompletionTokens = totalTokens - estimatedPromptTokens;
                            
                            // Calculer le coût des tokens
                            double tokenCostDouble = (estimatedPromptTokens * PRICE_INPUT_USD) + (estimatedCompletionTokens * PRICE_OUTPUT_USD);
                            tokenCost = BigDecimal.valueOf(tokenCostDouble);
                            
                            log.debug("Calcul coût tokens pour log ID {}: totalTokens={}, estimatedPrompt={}, estimatedCompletion={}, tokenCost={}", 
                                usageLog.getId(), totalTokens, estimatedPromptTokens, estimatedCompletionTokens, tokenCost);
                        }
                        
                        // Log pour diagnostic
                        log.debug("Calcul coût pour log ID {}: requestPrice={} (devise marché), baseCost={}, tokensUsed={}, tokenCostUsd={}", 
                            usageLog.getId(), requestPrice, baseCost, usageLog.getTokensUsed(), tokenCost);
                        
                        // Arrondir à 5 décimales pour le coût des tokens, 3 décimales pour le prix de la requête
                        BigDecimal tokenCostRounded = tokenCost.setScale(5, RoundingMode.HALF_UP);
                        BigDecimal requestPriceRounded = requestPrice.setScale(3, RoundingMode.HALF_UP);
                        
                        logMap.put("tokenCostUsd", tokenCostRounded.doubleValue()); // Coût des tokens en USD (pour admins uniquement)
                        logMap.put("totalCostUsd", requestPriceRounded.doubleValue()); // Prix de la requête dans devise marché
                        logMap.put("baseCostUsd", baseCost.setScale(3, RoundingMode.HALF_UP).doubleValue()); // BASE_REQUEST_PRICE
                        logMap.put("timestamp", usageLog.getTimestamp().toString());
                        return logMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("organizationId", organization.getId());
            response.put("organizationName", organization.getName());
            response.put("startDate", startDateTime.toString());
            response.put("endDate", endDateTime.toString());
            response.put("totalRequests", usageLogsWithNames.size());
            response.put("usageLogs", usageLogsWithNames);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Erreur lors de la récupération des logs d'utilisation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "ORGANIZATION_NOT_FOUND", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des logs d'utilisation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Erreur lors de la récupération des logs d'utilisation"));
        }
    }

    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("sub");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ID utilisateur courant", e);
        }
        return null;
    }
}

