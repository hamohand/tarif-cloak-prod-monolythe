package com.muhend.backend.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muhend.backend.alert.dto.QuotaAlertDto;
import com.muhend.backend.alert.model.QuotaAlert;
import com.muhend.backend.alert.repository.QuotaAlertRepository;
import com.muhend.backend.organization.dto.OrganizationDto;
import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.pricing.dto.PricingPlanDto;
import com.muhend.backend.pricing.service.PricingPlanService;
import com.muhend.backend.usage.repository.UsageLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service pour gérer les alertes de quota.
 * Détecte automatiquement les organisations dont le quota approche ou dépasse la limite.
 */
@Service
@Slf4j
public class QuotaAlertService {
    
    private static final String DEBUG_LOG_PATH = "c:\\Users\\hamoh\\Documents\\projets\\tarif\\tarif-saas\\tarif-cloak-prod\\.cursor\\debug.log";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static void debugLog(String location, String message, Map<String, Object> data, String hypothesisId) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("id", "log_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
            logEntry.put("timestamp", System.currentTimeMillis());
            logEntry.put("location", location);
            logEntry.put("message", message);
            logEntry.put("data", data);
            logEntry.put("sessionId", "debug-session");
            logEntry.put("runId", "run1");
            logEntry.put("hypothesisId", hypothesisId);
            try (PrintWriter writer = new PrintWriter(new FileWriter(DEBUG_LOG_PATH, true))) {
                writer.println(objectMapper.writeValueAsString(logEntry));
            }
        } catch (Exception e) {
            // Ignorer les erreurs de logging pour ne pas perturber le flux principal
        }
    }
    
    private final QuotaAlertRepository quotaAlertRepository;
    private final OrganizationService organizationService;
    private final UsageLogRepository usageLogRepository;
    private final PricingPlanService pricingPlanService;
    
    // Seuils d'alerte
    private static final double WARNING_THRESHOLD = 80.0;  // Alerte à 80%
    private static final double CRITICAL_THRESHOLD = 100.0; // Alerte à 100%
    
    public QuotaAlertService(
            QuotaAlertRepository quotaAlertRepository,
            OrganizationService organizationService,
            UsageLogRepository usageLogRepository,
            PricingPlanService pricingPlanService) {
        this.quotaAlertRepository = quotaAlertRepository;
        this.organizationService = organizationService;
        this.usageLogRepository = usageLogRepository;
        this.pricingPlanService = pricingPlanService;
    }
    
    /**
     * Vérifie les quotas de toutes les organisations et crée des alertes si nécessaire.
     * Cette méthode est appelée automatiquement toutes les heures.
     */
    @Scheduled(fixedRate = 3600000) // Toutes les heures (3600000 ms)
    @Transactional
    public void checkAllOrganizations() {
        log.debug("Vérification automatique des quotas pour toutes les organisations");
        List<OrganizationDto> organizations = organizationService.getAllOrganizations();
        
        for (OrganizationDto org : organizations) {
            if (org.getMonthlyQuota() != null) {
                checkOrganizationQuota(org.getId());
            }
        }
    }
    
    /**
     * Vérifie le quota d'une organisation spécifique et crée une alerte si nécessaire.
     * 
     * RÈGLE IMPORTANTE : monthlyQuota = null signifie quota ILLIMITÉ (pas d'alerte créée).
     * 
     * IMPORTANT : Les alertes sont basées sur :
     * - La consommation de l'organisation : somme de toutes les requêtes de tous les collaborateurs
     * - Le quota de l'organisation : défini par le plan tarifaire choisi (organization.monthlyQuota)
     * 
     * Les alertes affichent uniquement : consommation-organisation / quota-organisation
     * Les alertes sont visibles par tous les collaborateurs de l'organisation.
     */
    @Transactional
    public void checkOrganizationQuota(Long organizationId) {
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            return; // Organisation introuvable
        }
        
        // Récupérer la valeur actuelle du quota depuis le plan tarifaire (pas celle stockée dans l'organisation)
        Integer currentMonthlyQuota = organization.getMonthlyQuota(); // Valeur par défaut (pour compatibilité)
        if (organization.getPricingPlanId() != null) {
            try {
                PricingPlanDto plan = pricingPlanService.getPricingPlanById(organization.getPricingPlanId());
                currentMonthlyQuota = plan.getMonthlyQuota(); // Utiliser la valeur actuelle du plan
            } catch (Exception e) {
                log.warn("Impossible de récupérer le plan {} pour l'organisation {}: {}", 
                        organization.getPricingPlanId(), organizationId, e.getMessage());
                // Utiliser la valeur stockée dans l'organisation en cas d'erreur
            }
        }
        
        // #region agent log
        Map<String, Object> logDataD1 = new HashMap<>();
        logDataD1.put("organizationId", organizationId);
        logDataD1.put("organizationMonthlyQuota", organization.getMonthlyQuota());
        logDataD1.put("planMonthlyQuota", currentMonthlyQuota);
        logDataD1.put("isNull", currentMonthlyQuota == null);
        debugLog("QuotaAlertService.java:73", "checkOrganizationQuota - checking monthlyQuota", logDataD1, "F");
        // #endregion
        
        if (currentMonthlyQuota == null) {
            return; // Pas de quota à vérifier (quota illimité)
        }
        
        // Le quota provient du plan tarifaire de l'organisation (organization.monthlyQuota)
        // qui est défini lors du changement de plan ou à la création de l'organisation
        
        // Calculer la consommation du mois en cours pour TOUTE l'organisation
        // (somme de toutes les requêtes de tous les collaborateurs)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        
        long currentUsage = usageLogRepository.countByOrganizationIdAndTimestampBetween(
                organizationId, startOfMonth, endOfMonth);
        
        // Calculer le pourcentage : consommation-organisation / quota-organisation (utiliser la valeur actuelle du plan)
        // #region agent log
        Map<String, Object> logDataD2 = new HashMap<>();
        logDataD2.put("currentUsage", currentUsage);
        logDataD2.put("organizationMonthlyQuota", organization.getMonthlyQuota());
        logDataD2.put("planMonthlyQuota", currentMonthlyQuota);
        debugLog("QuotaAlertService.java:91", "checkOrganizationQuota - calculating percentage", logDataD2, "F");
        // #endregion
        double percentageUsed = (double) currentUsage / currentMonthlyQuota * 100;
        
        // Déterminer le type d'alerte
        QuotaAlert.AlertType alertType;
        String message;
        
        if (percentageUsed >= CRITICAL_THRESHOLD) {
            if (currentUsage > currentMonthlyQuota) {
                alertType = QuotaAlert.AlertType.EXCEEDED;
                message = String.format(
                    "⚠️ Le quota mensuel de votre organisation '%s' a été DÉPASSÉ ! Consommation de l'organisation: %d/%d requêtes (%.1f%%)",
                    organization.getName(), currentUsage, currentMonthlyQuota, percentageUsed
                );
            } else {
                alertType = QuotaAlert.AlertType.CRITICAL;
                message = String.format(
                    "🔴 Le quota mensuel de votre organisation '%s' a été ATTEINT ! Consommation de l'organisation: %d/%d requêtes (100%%)",
                    organization.getName(), currentUsage, currentMonthlyQuota
                );
            }
        } else if (percentageUsed >= WARNING_THRESHOLD) {
            alertType = QuotaAlert.AlertType.WARNING;
            message = String.format(
                "🟡 Le quota mensuel de votre organisation '%s' approche de la limite ! Consommation de l'organisation: %d/%d requêtes (%.1f%%)",
                organization.getName(), currentUsage, currentMonthlyQuota, percentageUsed
            );
        } else {
            // Pas d'alerte nécessaire, sortir de la méthode
            return;
        }
        
        // Créer une alerte si nécessaire (alertType est maintenant final)
        {
            // Récupérer toutes les alertes non lues pour cette organisation ce mois-ci
            List<QuotaAlert> existingAlerts = quotaAlertRepository.findByOrganizationIdAndIsReadFalseOrderByCreatedAtDesc(organizationId);
            
            // Filtrer les alertes du mois en cours
            List<QuotaAlert> currentMonthAlerts = existingAlerts.stream()
                    .filter(alert -> alert.getCreatedAt().getMonth() == now.getMonth() &&
                                   alert.getCreatedAt().getYear() == now.getYear())
                    .collect(java.util.stream.Collectors.toList());
            
            // Trouver l'alerte la plus critique existante
            QuotaAlert.AlertType mostCriticalExisting = null;
            if (!currentMonthAlerts.isEmpty()) {
                mostCriticalExisting = currentMonthAlerts.stream()
                        .map(QuotaAlert::getAlertType)
                        .max(this::compareAlertTypeSeverity)
                        .orElse(null);
            }
            
            // Déterminer si on doit créer une nouvelle alerte
            // Ordre de priorité : EXCEEDED > CRITICAL > WARNING
            boolean shouldCreateAlert = false;
            
            if (mostCriticalExisting == null) {
                // Aucune alerte existante, créer la nouvelle
                shouldCreateAlert = true;
            } else {
                // Comparer avec l'alerte existante la plus critique
                int comparison = compareAlertTypeSeverity(alertType, mostCriticalExisting);
                if (comparison > 0) {
                    // La nouvelle alerte est plus critique, marquer les anciennes comme lues et créer la nouvelle
                    for (QuotaAlert existingAlert : currentMonthAlerts) {
                        quotaAlertRepository.markAsRead(existingAlert.getId());
                        log.debug("Alerte {} marquée comme lue car remplacée par une alerte plus critique ({})", 
                                existingAlert.getAlertType(), alertType);
                    }
                    shouldCreateAlert = true;
                } else if (comparison < 0) {
                    // La nouvelle alerte est moins critique, ne pas créer (garder la plus critique)
                    shouldCreateAlert = false;
                    log.debug("Alerte {} ignorée car une alerte plus critique ({}) existe déjà", 
                            alertType, mostCriticalExisting);
                } else {
                    // Même niveau de criticité, vérifier si c'est exactement le même type
                    boolean sameTypeExists = currentMonthAlerts.stream()
                            .anyMatch(alert -> alert.getAlertType() == alertType);
                    if (!sameTypeExists) {
                        shouldCreateAlert = true;
                    } else {
                        // Même type existe déjà, ne pas créer de doublon
                        shouldCreateAlert = false;
                        log.debug("Alerte {} ignorée car une alerte du même type existe déjà", alertType);
                    }
                }
            }
            
            if (shouldCreateAlert) {
                QuotaAlert alert = new QuotaAlert();
                alert.setOrganizationId(organizationId);
                alert.setOrganizationName(organization.getName());
                alert.setAlertType(alertType);
                alert.setCurrentUsage(currentUsage);
                alert.setMonthlyQuota(currentMonthlyQuota); // Utiliser la valeur actuelle du plan
                alert.setPercentageUsed(percentageUsed);
                alert.setMessage(message);
                alert.setIsRead(false);
                
                quotaAlertRepository.save(alert);
                log.info("Alerte de quota créée: {} pour l'organisation {} ({}%)", 
                        alertType, organization.getName(), String.format("%.1f", percentageUsed));
            }
        }
    }
    
    /**
     * Récupère les alertes non lues pour une organisation.
     */
    public List<QuotaAlertDto> getUnreadAlertsForOrganization(Long organizationId) {
        return quotaAlertRepository.findByOrganizationIdAndIsReadFalseOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère toutes les alertes non lues (pour les admins).
     */
    public List<QuotaAlertDto> getAllUnreadAlerts() {
        return quotaAlertRepository.findByIsReadFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère toutes les alertes d'une organisation.
     */
    public List<QuotaAlertDto> getAllAlertsForOrganization(Long organizationId) {
        return quotaAlertRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Marque une alerte comme lue.
     */
    @Transactional
    public void markAlertAsRead(Long alertId) {
        quotaAlertRepository.markAsRead(alertId);
        log.debug("Alerte {} marquée comme lue", alertId);
    }
    
    /**
     * Marque toutes les alertes d'une organisation comme lues.
     */
    @Transactional
    public void markAllAlertsAsReadForOrganization(Long organizationId) {
        quotaAlertRepository.markAllAsReadForOrganization(organizationId);
        log.debug("Toutes les alertes de l'organisation {} marquées comme lues", organizationId);
    }
    
    /**
     * Compte les alertes non lues pour une organisation.
     */
    public long countUnreadAlertsForOrganization(Long organizationId) {
        return quotaAlertRepository.countByOrganizationIdAndIsReadFalse(organizationId);
    }
    
    /**
     * Compte toutes les alertes non lues (pour les admins).
     */
    public long countAllUnreadAlerts() {
        return quotaAlertRepository.countByIsReadFalse();
    }
    
    /**
     * Compare deux types d'alerte pour déterminer lequel est le plus critique.
     * @return valeur positive si alertType1 est plus critique, négative si alertType2 est plus critique, 0 si égaux
     * Ordre de criticité : EXCEEDED > CRITICAL > WARNING
     */
    private int compareAlertTypeSeverity(QuotaAlert.AlertType alertType1, QuotaAlert.AlertType alertType2) {
        if (alertType1 == alertType2) {
            return 0;
        }
        
        // Définir l'ordre de criticité
        int severity1 = getAlertTypeSeverity(alertType1);
        int severity2 = getAlertTypeSeverity(alertType2);
        
        return Integer.compare(severity1, severity2);
    }
    
    /**
     * Retourne un score de criticité pour un type d'alerte.
     * Plus le score est élevé, plus l'alerte est critique.
     */
    private int getAlertTypeSeverity(QuotaAlert.AlertType alertType) {
        switch (alertType) {
            case EXCEEDED:
                return 3;
            case CRITICAL:
                return 2;
            case WARNING:
                return 1;
            default:
                return 0;
        }
    }
    
    /**
     * Convertit une QuotaAlert en DTO.
     */
    private QuotaAlertDto toDto(QuotaAlert alert) {
        QuotaAlertDto dto = new QuotaAlertDto();
        dto.setId(alert.getId());
        dto.setOrganizationId(alert.getOrganizationId());
        dto.setOrganizationName(alert.getOrganizationName());
        dto.setAlertType(alert.getAlertType());
        dto.setCurrentUsage(alert.getCurrentUsage());
        dto.setMonthlyQuota(alert.getMonthlyQuota());
        dto.setPercentageUsed(alert.getPercentageUsed());
        dto.setMessage(alert.getMessage());
        dto.setIsRead(alert.getIsRead());
        dto.setCreatedAt(alert.getCreatedAt());
        return dto;
    }
}

