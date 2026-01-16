package com.muhend.backend.admin.service;

import com.muhend.backend.alert.repository.QuotaAlertRepository;
import com.muhend.backend.organization.model.OrganizationUser;
import com.muhend.backend.organization.repository.OrganizationUserRepository;
import com.muhend.backend.usage.repository.UsageLogRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour nettoyer les données des utilisateurs avec les rôles ORGANIZATION ou COLLABORATOR
 */
@Service
public class UserCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserCleanupService.class);
    
    private final Keycloak keycloak;
    private final UsageLogRepository usageLogRepository;
    private final QuotaAlertRepository quotaAlertRepository;
    private final OrganizationUserRepository organizationUserRepository;
    
    @Value("${keycloak.admin.realm:hscode-realm}")
    private String realm;
    
    public UserCleanupService(
            Keycloak keycloak,
            UsageLogRepository usageLogRepository,
            QuotaAlertRepository quotaAlertRepository,
            OrganizationUserRepository organizationUserRepository) {
        this.keycloak = keycloak;
        this.usageLogRepository = usageLogRepository;
        this.quotaAlertRepository = quotaAlertRepository;
        this.organizationUserRepository = organizationUserRepository;
    }
    
    /**
     * Nettoie tous les utilisateurs avec les rôles ORGANIZATION ou COLLABORATOR
     * @return Résultat du nettoyage avec statistiques
     */
    @Transactional
    public CleanupResult cleanupUsersWithRoles() {
        logger.info("=== Début du nettoyage des utilisateurs avec rôles ORGANIZATION ou COLLABORATOR ===");
        
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersResource = realmResource.users();
        
        // Récupérer tous les utilisateurs
        List<UserRepresentation> allUsers = usersResource.list();
        logger.info("Nombre total d'utilisateurs dans Keycloak: {}", allUsers.size());
        
        // Filtrer les utilisateurs avec les rôles ORGANIZATION ou COLLABORATOR
        List<String> userIdsToDelete = allUsers.stream()
            .filter(user -> {
                try {
                    boolean hasOrgRole = hasRole(user.getId(), "ORGANIZATION");
                    boolean hasCollabRole = hasRole(user.getId(), "COLLABORATOR");
                    boolean shouldDelete = hasOrgRole || hasCollabRole;
                    
                    if (shouldDelete) {
                        logger.info("Utilisateur {} trouvé avec rôles - ORGANIZATION: {}, COLLABORATOR: {}", 
                            user.getUsername(), hasOrgRole, hasCollabRole);
                    }
                    
                    return shouldDelete;
                } catch (Exception e) {
                    logger.warn("Erreur lors de la vérification des rôles pour l'utilisateur {}: {}", 
                        user.getUsername(), e.getMessage());
                    return false;
                }
            })
            .map(UserRepresentation::getId)
            .collect(Collectors.toList());
        
        logger.info("Nombre d'utilisateurs à supprimer: {}", userIdsToDelete.size());
        
        if (!userIdsToDelete.isEmpty()) {
            logger.info("IDs des utilisateurs à supprimer: {}", userIdsToDelete);
        }
        
        if (userIdsToDelete.isEmpty()) {
            logger.info("Aucun utilisateur à supprimer");
            return new CleanupResult(0, 0, 0);
        }
        
        int deletedCount = 0;
        int errorCount = 0;
        
        for (String keycloakUserId : userIdsToDelete) {
            try {
                UserResource userResource = usersResource.get(keycloakUserId);
                UserRepresentation user = userResource.toRepresentation();
                
                logger.info("Suppression de l'utilisateur: {} (ID: {})", user.getUsername(), keycloakUserId);
                
                // 1. Supprimer les logs d'utilisation
                long deletedLogs = usageLogRepository.deleteByKeycloakUserId(keycloakUserId);
                logger.debug("  - {} logs d'utilisation supprimés", deletedLogs);
                
                // 2. Récupérer les IDs des organisations associées avant suppression
                List<OrganizationUser> orgUsers = organizationUserRepository.findByKeycloakUserId(keycloakUserId);
                List<Long> organizationIds = orgUsers.stream()
                    .map(ou -> ou.getOrganization().getId())
                    .collect(Collectors.toList());
                
                // 3. Supprimer les associations organisation-utilisateur
                long deletedAssociations = organizationUserRepository.deleteByKeycloakUserId(keycloakUserId);
                logger.debug("  - {} associations organisation-utilisateur supprimées", deletedAssociations);
                
                // 4. Supprimer les alertes de quota pour ces organisations
                for (Long orgId : organizationIds) {
                    long deletedAlerts = quotaAlertRepository.deleteByOrganizationId(orgId);
                    if (deletedAlerts > 0) {
                        logger.debug("  - {} alertes de quota supprimées pour l'organisation {}", deletedAlerts, orgId);
                    }
                }
                
                // 5. Supprimer l'utilisateur de Keycloak
                userResource.remove();
                logger.info("  ✓ Utilisateur {} supprimé avec succès", user.getUsername());
                
                deletedCount++;
            } catch (Exception e) {
                logger.error("Erreur lors de la suppression de l'utilisateur {}: {}", 
                    keycloakUserId, e.getMessage(), e);
                errorCount++;
            }
        }
        
        logger.info("=== Fin du nettoyage ===");
        logger.info("Utilisateurs supprimés avec succès: {}", deletedCount);
        logger.info("Erreurs rencontrées: {}", errorCount);
        
        return new CleanupResult(deletedCount, errorCount, userIdsToDelete.size());
    }
    
    /**
     * Vérifie si un utilisateur a un rôle spécifique (ORGANIZATION ou COLLABORATOR)
     * Les rôles sont stockés au niveau du realm, pas au niveau du client
     */
    private boolean hasRole(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(userId);
            
            // Vérifier les rôles du realm (pas du client)
            List<RoleRepresentation> realmRoles = userResource.roles()
                .realmLevel()
                .listAll();
            
            boolean hasRole = realmRoles.stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
            
            if (hasRole) {
                logger.debug("Utilisateur {} a le rôle {}", userId, roleName);
            }
            
            return hasRole;
        } catch (Exception e) {
            logger.warn("Erreur lors de la vérification du rôle {} pour l'utilisateur {}: {}", 
                roleName, userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Classe pour stocker le résultat du nettoyage
     */
    public static class CleanupResult {
        private final int deletedCount;
        private final int errorCount;
        private final int totalFound;
        
        public CleanupResult(int deletedCount, int errorCount, int totalFound) {
            this.deletedCount = deletedCount;
            this.errorCount = errorCount;
            this.totalFound = totalFound;
        }
        
        public int getDeletedCount() {
            return deletedCount;
        }
        
        public int getErrorCount() {
            return errorCount;
        }
        
        public int getTotalFound() {
            return totalFound;
        }
    }
}

