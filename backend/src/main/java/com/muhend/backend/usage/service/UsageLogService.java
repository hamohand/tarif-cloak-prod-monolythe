package com.muhend.backend.usage.service;

import com.muhend.backend.usage.model.UsageLog;
import com.muhend.backend.usage.repository.UsageLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour gérer les logs d'utilisation.
 * Phase 1 MVP : Enregistrement simple des recherches.
 */
@Service
@Slf4j
public class UsageLogService {
    
    private final UsageLogRepository repository;
    
    public UsageLogService(UsageLogRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Enregistre un log d'utilisation.
     * Cette méthode est complètement non-bloquante : si la table n'existe pas ou en cas d'erreur,
     * elle logue un warning mais ne lève jamais d'exception pour ne pas faire échouer la requête principale.
     * 
     * @param keycloakUserId ID de l'utilisateur Keycloak
     * @param organizationId ID de l'organisation (peut être null)
     * @param endpoint Endpoint appelé (ex: "/recherche/sections")
     * @param searchTerm Terme de recherche
     * @param tokens Nombre de tokens utilisés
     * @param costUsd Coût en USD
     */
    public void logUsage(String keycloakUserId, Long organizationId, String endpoint, String searchTerm, 
                        Integer tokens, Double costUsd) {
        // Convertir Double en BigDecimal pour la précision monétaire
        BigDecimal costUsdDecimal = costUsd != null ? BigDecimal.valueOf(costUsd) : null;
        logUsageInternal(keycloakUserId, organizationId, endpoint, searchTerm, tokens, costUsdDecimal);
    }
    
    /**
     * Méthode interne pour enregistrer un log avec BigDecimal.
     */
    private void logUsageInternal(String keycloakUserId, Long organizationId, String endpoint, String searchTerm, 
                                  Integer tokens, BigDecimal costUsd) {
        try {
            UsageLog usageLog = new UsageLog();
            usageLog.setKeycloakUserId(keycloakUserId);
            usageLog.setOrganizationId(organizationId);
            usageLog.setEndpoint(endpoint);
            usageLog.setSearchTerm(searchTerm);
            usageLog.setTokensUsed(tokens);
            usageLog.setCostUsd(costUsd);
            usageLog.setTimestamp(LocalDateTime.now());
            
            repository.save(usageLog);
            log.debug("Usage log enregistré pour l'utilisateur: {}, endpoint: {}, coût: {} EUR", 
                     keycloakUserId, endpoint, costUsd != null ? costUsd : BigDecimal.ZERO);
        } catch (org.springframework.dao.DataAccessException e) {
            // Erreur de base de données (table absente, connexion, etc.) - non bloquant
            log.warn("Impossible d'enregistrer le log d'utilisation en base de données (table peut-être absente ou erreur DB): {}", 
                    e.getMessage());
        } catch (Exception e) {
            // Toute autre exception - non bloquant
            log.warn("Erreur lors de l'enregistrement du log d'utilisation (non bloquant): {}", e.getMessage());
        }
    }
    
    /**
     * Récupère tous les logs d'un utilisateur.
     */
    public List<UsageLog> getUsageLogsByUser(String keycloakUserId) {
        return repository.findByKeycloakUserId(keycloakUserId);
    }
    
    /**
     * Récupère les logs entre deux dates.
     */
    public List<UsageLog> getUsageLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByTimestampBetween(start, end);
    }
    
    /**
     * Récupère les logs d'un utilisateur entre deux dates.
     */
    public List<UsageLog> getUsageLogsByUserAndDateRange(String keycloakUserId, 
                                                          LocalDateTime start, 
                                                          LocalDateTime end) {
        return repository.findByKeycloakUserIdAndTimestampBetween(keycloakUserId, start, end);
    }
    
    /**
     * Récupère tous les logs (pour l'admin).
     */
    public List<UsageLog> getAllUsageLogs() {
        return repository.findAll();
    }
    
    /**
     * Récupère les logs d'une organisation entre deux dates.
     */
    public List<UsageLog> getUsageLogsByOrganizationAndDateRange(Long organizationId, 
                                                                  LocalDateTime start, 
                                                                  LocalDateTime end) {
        return repository.findByOrganizationIdAndTimestampBetween(organizationId, start, end);
    }
    
    /**
     * Récupère les logs d'une organisation.
     */
    public List<UsageLog> getUsageLogsByOrganization(Long organizationId) {
        return repository.findAll().stream()
                .filter(log -> log.getOrganizationId() != null && log.getOrganizationId().equals(organizationId))
                .collect(java.util.stream.Collectors.toList());
    }
}

