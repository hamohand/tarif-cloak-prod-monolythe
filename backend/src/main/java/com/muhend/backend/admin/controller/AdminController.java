package com.muhend.backend.admin.controller;

import com.muhend.backend.admin.service.OrganizationDeletionService;
import com.muhend.backend.auth.model.PendingRegistration;
import com.muhend.backend.auth.service.PendingRegistrationService;
import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.usage.model.UsageLog;
import com.muhend.backend.usage.repository.UsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur admin pour les opérations de maintenance
 * ⚠️ ATTENTION : Toutes les opérations sont irréversibles !
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final OrganizationDeletionService organizationDeletionService;
    private final PendingRegistrationService pendingRegistrationService;
    private final UsageLogRepository usageLogRepository;
    private final OrganizationService organizationService;
    
    public AdminController(
        OrganizationDeletionService organizationDeletionService,
        PendingRegistrationService pendingRegistrationService,
        UsageLogRepository usageLogRepository,
        OrganizationService organizationService
    ) {
        this.organizationDeletionService = organizationDeletionService;
        this.pendingRegistrationService = pendingRegistrationService;
        this.usageLogRepository = usageLogRepository;
        this.organizationService = organizationService;
    }
    
    /**
     * Supprime définitivement une organisation et tous ses éléments associés
     * ⚠️ ATTENTION : Cette opération est irréversible !
     * 
     * Supprime :
     * - Les éléments de facture (invoice_item)
     * - Les factures (invoice)
     * - Les paiements (payment)
     * - Les abonnements (subscription)
     * - Les demandes de devis (quote_request)
     * - Les logs d'utilisation (usage_log)
     * - Les alertes de quota (quota_alert)
     * - Les associations utilisateur-organisation (organization_user)
     * - L'organisation elle-même (organization)
     * 
     * @param organizationId ID de l'organisation à supprimer
     * @return Statistiques de la suppression
     */
    @DeleteMapping("/organizations/{organizationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteOrganization(@PathVariable Long organizationId) {
        logger.warn("=== DÉMARRAGE DE LA SUPPRESSION DE L'ORGANISATION {} ===", organizationId);
        logger.warn("Cette opération va supprimer définitivement l'organisation et tous ses éléments associés");
        
        try {
            OrganizationDeletionService.DeletionResult result = organizationDeletionService.deleteOrganization(organizationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", "Organisation supprimée avec succès");
            response.put("organizationId", result.getOrganizationId());
            response.put("organizationName", result.getOrganizationName());
            response.put("deletedInvoiceItems", result.getDeletedInvoiceItems());
            response.put("deletedInvoices", result.getDeletedInvoices());
            response.put("deletedPayments", result.getDeletedPayments());
            response.put("deletedSubscriptions", result.getDeletedSubscriptions());
            response.put("deletedQuoteRequests", result.getDeletedQuoteRequests());
            response.put("deletedUsageLogs", result.getDeletedUsageLogs());
            response.put("deletedQuotaAlerts", result.getDeletedQuotaAlerts());
            response.put("deletedOrganizationUsers", result.getDeletedOrganizationUsers());
            
            logger.info("Suppression terminée: organisation {} supprimée", result.getOrganizationName());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Organisation non trouvée: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'organisation {}", organizationId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Erreur lors de la suppression: " + e.getMessage()
            ));
        }
    }

    /**
     * Récupère tous les utilisateurs en attente d'inscription
     * @return Liste des inscriptions en attente
     */
    @GetMapping("/pending-registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PendingRegistration>> getPendingRegistrations() {
        logger.info("Récupération de tous les utilisateurs en attente d'inscription");
        List<PendingRegistration> pending = pendingRegistrationService.getAllPendingRegistrations();
        return ResponseEntity.ok(pending);
    }
    
    /**
     * Supprime une inscription en attente spécifique
     * @param id ID de l'inscription en attente à supprimer
     * @return Message de confirmation
     */
    @DeleteMapping("/pending-registrations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePendingRegistration(@PathVariable Long id) {
        logger.info("Suppression de l'inscription en attente avec l'ID: {}", id);
        try {
            pendingRegistrationService.deletePendingRegistration(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Inscription en attente supprimée avec succès"
            ));
        } catch (IllegalArgumentException e) {
            logger.error("Inscription en attente non trouvée: {}", e.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'inscription en attente {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Erreur lors de la suppression: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Supprime toutes les inscriptions en attente expirées
     * @return Nombre d'inscriptions supprimées
     */
    @DeleteMapping("/pending-registrations/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteExpiredPendingRegistrations() {
        logger.info("Suppression de toutes les inscriptions en attente expirées");
        try {
            pendingRegistrationService.cleanupExpiredRegistrations();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Nettoyage des inscriptions expirées terminé"
            ));
        } catch (Exception e) {
            logger.error("Erreur lors du nettoyage des inscriptions expirées", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Erreur lors du nettoyage: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Récupère les statistiques d'utilisation globales ou pour une organisation spécifique
     * @param organizationId ID de l'organisation (optionnel)
     * @param startDate Date de début (optionnel, format: yyyy-MM-dd)
     * @param endDate Date de fin (optionnel, format: yyyy-MM-dd)
     * @return Statistiques d'utilisation
     */
    @GetMapping("/usage/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsageStats(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        logger.info("Récupération des statistiques d'utilisation - organizationId: {}, startDate: {}, endDate: {}", 
                organizationId, startDate, endDate);
        
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
        
        // Récupérer les logs selon les filtres
        List<UsageLog> logs;
        if (organizationId != null) {
            logs = usageLogRepository.findByOrganizationIdAndTimestampBetween(organizationId, startDateTime, endDateTime);
        } else {
            logs = usageLogRepository.findByTimestampBetween(startDateTime, endDateTime);
        }
        
        // Calculer les statistiques globales
        long totalRequests = logs.size();
        BigDecimal totalCostUsd = logs.stream()
                .filter(log -> log.getCostUsd() != null)
                .map(UsageLog::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalTokens = logs.stream()
                .filter(log -> log.getTokensUsed() != null)
                .mapToLong(UsageLog::getTokensUsed)
                .sum();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRequests", totalRequests);
        response.put("totalCostUsd", totalCostUsd.doubleValue());
        response.put("totalTokens", totalTokens);
        
        // Statistiques par organisation (si pas de filtre organisation)
        if (organizationId == null) {
            Map<Long, List<UsageLog>> logsByOrg = logs.stream()
                    .filter(log -> log.getOrganizationId() != null)
                    .collect(Collectors.groupingBy(UsageLog::getOrganizationId));
            
            List<Map<String, Object>> statsByOrganization = new ArrayList<>();
            for (Map.Entry<Long, List<UsageLog>> entry : logsByOrg.entrySet()) {
                Long orgId = entry.getKey();
                List<UsageLog> orgLogs = entry.getValue();
                
                try {
                    var orgDto = organizationService.getOrganizationById(orgId);
                    if (orgDto != null) {
                        Map<String, Object> orgStats = new LinkedHashMap<>();
                        orgStats.put("organizationId", orgId);
                        orgStats.put("organizationName", orgDto.getName());
                        orgStats.put("requestCount", orgLogs.size());
                        orgStats.put("totalCostUsd", orgLogs.stream()
                                .filter(log -> log.getCostUsd() != null)
                                .map(UsageLog::getCostUsd)
                                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
                        orgStats.put("totalTokens", orgLogs.stream()
                                .filter(log -> log.getTokensUsed() != null)
                                .mapToLong(UsageLog::getTokensUsed)
                                .sum());
                        statsByOrganization.add(orgStats);
                    }
                } catch (Exception e) {
                    logger.warn("Impossible de récupérer l'organisation {}: {}", orgId, e.getMessage());
                }
            }
            response.put("statsByOrganization", statsByOrganization);
        }
        
        // Statistiques par utilisateur
        Map<String, List<UsageLog>> logsByUser = logs.stream()
                .collect(Collectors.groupingBy(UsageLog::getKeycloakUserId));
        
        List<Map<String, Object>> statsByUser = new ArrayList<>();
        for (Map.Entry<String, List<UsageLog>> entry : logsByUser.entrySet()) {
            String userId = entry.getKey();
            List<UsageLog> userLogs = entry.getValue();
            
            Map<String, Object> userStats = new LinkedHashMap<>();
            userStats.put("keycloakUserId", userId);
            userStats.put("requestCount", userLogs.size());
            userStats.put("totalCostUsd", userLogs.stream()
                    .filter(log -> log.getCostUsd() != null)
                    .map(UsageLog::getCostUsd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
            userStats.put("totalTokens", userLogs.stream()
                    .filter(log -> log.getTokensUsed() != null)
                    .mapToLong(UsageLog::getTokensUsed)
                    .sum());
            statsByUser.add(userStats);
        }
        response.put("statsByUser", statsByUser);
        
        // Utilisations récentes (10 dernières)
        List<Map<String, Object>> recentUsage = logs.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10)
                .map(log -> {
                    Map<String, Object> logMap = new LinkedHashMap<>();
                    logMap.put("id", log.getId());
                    logMap.put("keycloakUserId", log.getKeycloakUserId());
                    logMap.put("organizationId", log.getOrganizationId());
                    logMap.put("endpoint", log.getEndpoint());
                    logMap.put("searchTerm", log.getSearchTerm());
                    logMap.put("tokensUsed", log.getTokensUsed());
                    logMap.put("costUsd", log.getCostUsd() != null ? log.getCostUsd().doubleValue() : null);
                    logMap.put("timestamp", log.getTimestamp().toString());
                    return logMap;
                })
                .collect(Collectors.toList());
        response.put("recentUsage", recentUsage);
        
        return ResponseEntity.ok(response);
    }
}
