package com.muhend.backend.auth.service;

import com.muhend.backend.auth.dto.UserRegistrationRequest;
import com.muhend.backend.auth.model.PendingRegistration;
import com.muhend.backend.auth.repository.PendingRegistrationRepository;
import com.muhend.backend.email.service.EmailService;
import com.muhend.backend.organization.dto.CreateCollaboratorRequest;
import com.muhend.backend.organization.model.Organization;
import com.muhend.backend.organization.repository.OrganizationRepository;
import com.muhend.backend.organization.service.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour gérer les inscriptions en attente de confirmation.
 */
@Service
@Slf4j
public class PendingRegistrationService {
    
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final KeycloakAdminService keycloakAdminService;
    private final EmailService emailService;
    
    @Value("${registration.confirmation.token.expiration.hours:24}")
    private int tokenExpirationHours;
    
    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    public PendingRegistrationService(
            PendingRegistrationRepository pendingRegistrationRepository,
            OrganizationRepository organizationRepository,
            OrganizationService organizationService,
            KeycloakAdminService keycloakAdminService,
            EmailService emailService) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.organizationRepository = organizationRepository;
        this.organizationService = organizationService;
        this.keycloakAdminService = keycloakAdminService;
        this.emailService = emailService;
    }
    
    /**
     * Crée une inscription en attente et envoie un email de confirmation.
     */
    @Transactional
    public PendingRegistration createPendingRegistration(UserRegistrationRequest request) {
        // Vérifier si l'organisation existe déjà
        boolean organizationExists = organizationRepository.existsByEmail(request.getOrganizationEmail());
        if (organizationExists) {
            throw new IllegalArgumentException("Une organisation avec cet email existe déjà. Veuillez vous connecter ou contacter l'administrateur.");
        }
        
        // Vérifier si un utilisateur avec cet email est déjà en cours d'inscription (non confirmé)
        if (pendingRegistrationRepository.existsByEmailAndNotConfirmed(request.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email est déjà en cours d'inscription.");
        }
        
        // Vérifier si une inscription en attente existe déjà pour cet email d'organisation (non confirmée)
        if (pendingRegistrationRepository.existsByOrganizationEmailAndNotConfirmed(request.getOrganizationEmail())) {
            throw new IllegalArgumentException("Une inscription en attente existe déjà pour cet email d'organisation.");
        }
        
        // Générer un token de confirmation
        String confirmationToken = generateConfirmationToken();
        
        // Créer l'inscription en attente
        log.info("=== Création PendingRegistration ===");
        log.info("Email utilisateur depuis request: {}", request.getEmail());
        log.info("Email organisation depuis request: {}", request.getOrganizationEmail());
        
        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(request.getUsername());
        pending.setEmail(request.getEmail());
        pending.setFirstName(request.getFirstName());
        pending.setLastName(request.getLastName());
        pending.setPassword(request.getPassword()); // TODO: Hasher le mot de passe si nécessaire
        pending.setOrganizationPassword(request.getOrganizationPassword()); // TODO: Hasher si nécessaire
        pending.setOrganizationName(request.getOrganizationName());
        pending.setOrganizationEmail(request.getOrganizationEmail());
        pending.setOrganizationAddress(request.getOrganizationAddress());
        if (request.getOrganizationActivityDomain() != null && !request.getOrganizationActivityDomain().trim().isEmpty()) {
            pending.setOrganizationActivityDomain(request.getOrganizationActivityDomain().trim());
        }
        pending.setOrganizationCountry(request.getOrganizationCountry().toUpperCase());
        pending.setOrganizationPhone(request.getOrganizationPhone());
        pending.setPricingPlanId(request.getPricingPlanId()); // Inclure le plan tarifaire sélectionné
        if (request.getMarketVersion() != null && !request.getMarketVersion().trim().isEmpty()) {
            pending.setMarketVersion(request.getMarketVersion().trim());
        }
        pending.setConfirmationToken(confirmationToken);
        pending.setExpiresAt(LocalDateTime.now().plusHours(tokenExpirationHours));
        pending.setConfirmed(false);
        
        log.info("Email utilisateur dans pending avant save: {}", pending.getEmail());
        log.info("Email organisation dans pending avant save: {}", pending.getOrganizationEmail());
        
        pending = pendingRegistrationRepository.save(pending);
        
        log.info("Email utilisateur dans pending après save: {}", pending.getEmail());
        log.info("Email organisation dans pending après save: {}", pending.getOrganizationEmail());
        log.info("Inscription en attente créée: id={}, organizationEmail={}, token={}", 
            pending.getId(), pending.getOrganizationEmail(), confirmationToken);
        
        // Envoyer l'email de confirmation
        sendConfirmationEmail(pending, false);
        
        return pending;
    }
    
    /**
     * Invite un collaborateur pour une organisation existante.
     */
    @Transactional
    public PendingRegistration inviteCollaborator(String organizationKeycloakUserId,
                                                  CreateCollaboratorRequest request) {
        Organization organization = organizationRepository.findByKeycloakUserId(organizationKeycloakUserId)
                .orElseThrow(() -> new IllegalArgumentException("Aucune organisation associée à ce compte."));
        
        if (pendingRegistrationRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Un utilisateur avec ce nom d'utilisateur est déjà en cours d'inscription.");
        }
        if (pendingRegistrationRepository.existsByEmailAndNotConfirmed(request.getEmail())) {
            throw new IllegalArgumentException("Un utilisateur avec cet email est déjà en cours d'inscription.");
        }
        String organizationEmail = organization.getEmail();
        if (organizationEmail != null && organizationEmail.equalsIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("L'email du collaborateur doit être différent de celui de l'organisation.");
        }
        
        String confirmationToken = generateConfirmationToken();
        String temporaryPassword = generateTemporaryPassword();
        
        // Vérifier si l'utilisateur existe déjà dans Keycloak et le désactiver si besoin
        String existingUserId = keycloakAdminService.getUserIdByUsername(request.getUsername());
        if (existingUserId != null) {
            try {
                keycloakAdminService.disableUser(existingUserId);
                log.info("Utilisateur {} existant désactivé dans Keycloak avant réinvitation", request.getUsername());
            } catch (Exception e) {
                log.warn("Impossible de désactiver l'utilisateur existant {}: {}", request.getUsername(), e.getMessage());
            }
        }
        
        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(request.getUsername());
        pending.setEmail(request.getEmail());
        pending.setFirstName(request.getFirstName());
        pending.setLastName(request.getLastName());
        pending.setPassword(temporaryPassword);
        pending.setOrganizationName(organization.getName());
        pending.setOrganizationEmail(organization.getEmail());
        pending.setOrganizationAddress(organization.getAddress());
        if (organization.getActivityDomain() != null && !organization.getActivityDomain().trim().isEmpty()) {
            pending.setOrganizationActivityDomain(organization.getActivityDomain().trim());
        }
        pending.setOrganizationCountry(organization.getCountry());
        pending.setOrganizationPhone(organization.getPhone());
        pending.setOrganizationPassword(null);
        pending.setPricingPlanId(organization.getPricingPlanId());
        pending.setConfirmationToken(confirmationToken);
        pending.setExpiresAt(LocalDateTime.now().plusHours(tokenExpirationHours));
        pending.setConfirmed(false);
        
        pending = pendingRegistrationRepository.save(pending);
        
        sendCollaboratorInvitationEmail(pending, organization);
        return pending;
    }
    
    /**
     * Confirme une inscription en attente et crée l'utilisateur et l'organisation.
     */
    @Transactional
    public void confirmRegistration(String token) {
        Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository
            .findByConfirmationToken(token);
        
        if (pendingOpt.isEmpty()) {
            throw new IllegalArgumentException("Token de confirmation invalide");
        }
        
        PendingRegistration pending = pendingOpt.get();
        
        // Vérifier si déjà confirmé
        if (pending.getConfirmed()) {
            throw new IllegalStateException("Cette inscription a déjà été confirmée");
        }
        
        // Vérifier si le token a expiré
        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Le token de confirmation a expiré");
        }
        
        // Vérifier si l'organisation existe déjà
        Optional<Organization> existingOrg = organizationRepository
            .findByEmail(pending.getOrganizationEmail());
        
        if (existingOrg.isPresent()) {
            // Organisation existe déjà : créer uniquement l'utilisateur et l'associer
            log.info("Organisation existante trouvée: {}", existingOrg.get().getEmail());
            createUserAndAssociateToOrganization(pending, existingOrg.get().getId());
        } else {
            // Organisation n'existe pas : créer l'organisation et l'utilisateur
            log.info("Création d'une nouvelle organisation: {}", pending.getOrganizationEmail());
            createOrganizationAndUser(pending);
        }
        
        // Marquer comme confirmé
        pending.setConfirmed(true);
        pendingRegistrationRepository.save(pending);
        
        log.info("Inscription confirmée avec succès: token={}", token);
    }
    
    /**
     * Crée une organisation et un utilisateur.
     */
    private void createOrganizationAndUser(PendingRegistration pending) {
        try {
            log.info("=== Création du compte organisation ===");
            jakarta.ws.rs.core.Response orgResponse = keycloakAdminService.createUser(
                    pending.getOrganizationEmail(),
                    pending.getOrganizationEmail(),
                    pending.getOrganizationPassword(),
                    pending.getOrganizationName(),
                    null,
                    true,
                    true,
                    null,  // Pas d'action requise - l'utilisateur peut utiliser le mot de passe envoyé par email
                    java.util.Map.of("account_type", java.util.List.of("ORGANIZATION"))
            );

            int orgStatus = orgResponse.getStatus();
            String organizationKeycloakUserId = null;
            if (orgStatus == jakarta.ws.rs.core.Response.Status.CREATED.getStatusCode()) {
                organizationKeycloakUserId = keycloakAdminService.getUserIdFromResponse(orgResponse);
                if (organizationKeycloakUserId == null) {
                    organizationKeycloakUserId = keycloakAdminService.getUserIdByUsername(pending.getOrganizationEmail());
                }
                orgResponse.close();
            } else if (orgStatus == jakarta.ws.rs.core.Response.Status.CONFLICT.getStatusCode()) {
                log.warn("Compte Keycloak organisation déjà existant pour {}", pending.getOrganizationEmail());
                orgResponse.close();
                organizationKeycloakUserId = keycloakAdminService.getUserIdByUsername(pending.getOrganizationEmail());
            } else {
                String errorBody = orgResponse.hasEntity() ? orgResponse.readEntity(String.class) : "N/A";
                orgResponse.close();
                throw new RuntimeException("Erreur lors de la création du compte organisation dans Keycloak (status="
                        + orgStatus + ", body=" + errorBody + ")");
            }

            if (organizationKeycloakUserId == null) {
                throw new RuntimeException("Impossible de récupérer l'ID Keycloak du compte organisation");
            }
            log.info("Compte Keycloak organisation créé/retourné: {}", organizationKeycloakUserId);

            // Assigner les rôles organisation à l'utilisateur (ORGANIZATION, USER, COLLABORATOR)
            // Les utilisateurs ORGANISATION ont aussi le rôle COLLABORATOR pour accéder aux fonctionnalités collaborateur
            keycloakAdminService.assignRealmRoles(organizationKeycloakUserId, java.util.List.of("ORGANIZATION", "USER", "COLLABORATOR"));

            // Créer l'organisation dans l'application
            com.muhend.backend.organization.dto.CreateOrganizationRequest orgRequest =
                    new com.muhend.backend.organization.dto.CreateOrganizationRequest();
            orgRequest.setName(pending.getOrganizationName());
            orgRequest.setEmail(pending.getOrganizationEmail());
            orgRequest.setAddress(pending.getOrganizationAddress());
            if (pending.getOrganizationActivityDomain() != null && !pending.getOrganizationActivityDomain().trim().isEmpty()) {
                orgRequest.setActivityDomain(pending.getOrganizationActivityDomain().trim());
            }
            orgRequest.setCountry(pending.getOrganizationCountry());
            orgRequest.setPhone(pending.getOrganizationPhone());
            orgRequest.setOrganizationPassword(pending.getOrganizationPassword());
            orgRequest.setKeycloakUserId(organizationKeycloakUserId);
            orgRequest.setPricingPlanId(pending.getPricingPlanId());
            if (pending.getMarketVersion() != null && !pending.getMarketVersion().trim().isEmpty()) {
                orgRequest.setMarketVersion(pending.getMarketVersion().trim());
            }

            var organizationDto = organizationService.createOrganization(orgRequest);
            log.info("Organisation créée: id={}, name={}", organizationDto.getId(), organizationDto.getName());

            // Associer le compte organisation à l'entité Organisation (pour les quotas/reporting)
            organizationService.addUserToOrganization(organizationDto.getId(), organizationKeycloakUserId);
            log.info("Compte organisation {} associé à l'organisation {}", organizationKeycloakUserId, organizationDto.getId());

        } catch (Exception e) {
            log.error("Erreur lors de la création de l'organisation: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création: " + e.getMessage(), e);
        }
    }
    
    /**
     * Crée un utilisateur et l'associe à une organisation existante.
     */
    private void createUserAndAssociateToOrganization(PendingRegistration pending, Long organizationId) {
        try {
            // 1. Créer l'utilisateur dans Keycloak
            log.info("=== Création utilisateur pour organisation existante ===");
            log.info("Email utilisateur depuis PendingRegistration: {}", pending.getEmail());
            log.info("Email organisation depuis PendingRegistration: {}", pending.getOrganizationEmail());
            
            log.info("Email utilisateur dans keycloakRequest: {}", pending.getEmail());
            
            jakarta.ws.rs.core.Response response = keycloakAdminService.createUser(
                    pending.getUsername(),
                    pending.getEmail(),
                    pending.getPassword(),
                    pending.getFirstName(),
                    pending.getLastName(),
                    true,
                    false,
                    null,  // Pas d'action requise - l'utilisateur peut utiliser le mot de passe envoyé par email
                    java.util.Map.of(
                            "account_type", java.util.List.of("COLLABORATOR"),
                            "organization_email", java.util.List.of(pending.getOrganizationEmail())
                    )
            );
            int status = response.getStatus();
            String keycloakUserId = null;
            if (status == jakarta.ws.rs.core.Response.Status.CREATED.getStatusCode()) {
                keycloakUserId = keycloakAdminService.getUserIdFromResponse(response);
                if (keycloakUserId == null) {
                    keycloakUserId = keycloakAdminService.getUserIdByUsername(pending.getUsername());
                }
                response.close();
            } else if (status == jakarta.ws.rs.core.Response.Status.CONFLICT.getStatusCode()) {
                log.warn("Utilisateur Keycloak déjà existant pour {}", pending.getUsername());
                response.close();
                keycloakUserId = keycloakAdminService.getUserIdByUsername(pending.getUsername());
            } else {
                String errorBody = response.hasEntity() ? response.readEntity(String.class) : "N/A";
                response.close();
                throw new RuntimeException("Erreur lors de la création de l'utilisateur dans Keycloak (status="
                        + status + ", body=" + errorBody + ")");
            }
            
            if (keycloakUserId == null) {
                throw new RuntimeException("Impossible de récupérer l'ID Keycloak de l'utilisateur créé");
            }
            
            log.info("Utilisateur Keycloak créé: {}", keycloakUserId);

            keycloakAdminService.assignRealmRoles(keycloakUserId, java.util.List.of("COLLABORATOR", "USER"));
            
            // 3. Associer l'utilisateur à l'organisation existante
            organizationService.addUserToOrganization(organizationId, keycloakUserId);
            log.info("Utilisateur {} associé à l'organisation existante {}", keycloakUserId, organizationId);
            
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur et de l'association: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création: " + e.getMessage(), e);
        }
    }
    
    /**
     * Génère un token de confirmation sécurisé.
     */
    private String generateConfirmationToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    private String generateTemporaryPassword() {
        final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder password = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            int index = secureRandom.nextInt(characters.length());
            password.append(characters.charAt(index));
        }
        return password.toString();
    }
    
    /**
     * Envoie un email de confirmation.
     */
    private void sendConfirmationEmail(PendingRegistration pending, boolean organizationExists) {
        try {
            log.info("Tentative d'envoi d'email de confirmation pour l'inscription en attente id={}, organizationEmail={}", 
                pending.getId(), pending.getOrganizationEmail());
            
            String confirmationUrl = frontendUrl + "/auth/confirm-registration?token=" + pending.getConfirmationToken();
            log.debug("URL de confirmation générée: {}", confirmationUrl);
            
            if (organizationExists) {
                // Email pour rejoindre une organisation existante
                log.info("Envoi d'email pour rejoindre une organisation existante: {}", pending.getOrganizationName());
                emailService.sendRegistrationConfirmationEmail(
                    pending.getOrganizationEmail(),
                    pending.getEmail(),
                    pending.getOrganizationName(),
                    confirmationUrl,
                    true // isExistingOrganization
                );
            } else {
                // Email pour créer une nouvelle organisation
                log.info("Envoi d'email pour créer une nouvelle organisation: {}", pending.getOrganizationName());
                emailService.sendRegistrationConfirmationEmail(
                    pending.getOrganizationEmail(),
                    pending.getEmail(),
                    pending.getOrganizationName(),
                    confirmationUrl,
                    false // isExistingOrganization
                );
            }
            
            log.info("✓ Email de confirmation envoyé avec succès à: {}", pending.getOrganizationEmail());
        } catch (IllegalStateException e) {
            // Erreur de configuration SMTP
            log.error("✗ ERREUR DE CONFIGURATION SMTP lors de l'envoi de l'email de confirmation: {}", e.getMessage());
            log.error("✗ Détails: {}", e.getCause() != null ? e.getCause().getMessage() : "Aucun détail supplémentaire");
            // Ne pas faire échouer la création de l'inscription en attente, mais logger l'erreur
            throw new RuntimeException("Impossible d'envoyer l'email de confirmation. Configuration SMTP manquante ou incorrecte: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("✗ ERREUR lors de l'envoi de l'email de confirmation à {}: {}", 
                pending.getOrganizationEmail(), e.getMessage(), e);
            log.error("✗ Type d'exception: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("✗ Cause: {}", e.getCause().getMessage());
            }
            // Ne pas faire échouer la création de l'inscription en attente si l'email échoue
            // mais lever une exception pour que l'utilisateur soit informé
            throw new RuntimeException("Impossible d'envoyer l'email de confirmation: " + e.getMessage(), e);
        }
    }
    
    private void sendCollaboratorInvitationEmail(PendingRegistration pending, Organization organization) {
        try {
            log.info("Envoi d'une invitation collaborateur pour l'organisation {} ({})", organization.getName(), organization.getEmail());
            String confirmationUrl = frontendUrl + "/auth/confirm-registration?token=" + pending.getConfirmationToken();
            emailService.sendCollaboratorInvitationEmail(
                    pending.getEmail(),
                    pending.getFirstName(),
                    pending.getLastName(),
                    pending.getUsername(),
                    organization.getName(),
                    organization.getEmail(),
                    confirmationUrl,
                    pending.getPassword()
            );
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'invitation collaborateur à {}: {}", pending.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'invitation collaborateur: " + e.getMessage(), e);
        }
    }
    
    /**
     * Nettoie les inscriptions expirées (peut être appelé par un scheduled task).
     */
    @Transactional
    public void cleanupExpiredRegistrations() {
        LocalDateTime now = LocalDateTime.now();
        List<PendingRegistration> expired = pendingRegistrationRepository.findExpiredUnconfirmed(now);
        log.info("Nettoyage de {} inscriptions expirées", expired.size());
        pendingRegistrationRepository.deleteExpiredUnconfirmed(now);
    }
    
    /**
     * Tâche planifiée pour nettoyer automatiquement les inscriptions expirées.
     * Exécutée tous les jours à 2h du matin.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void scheduledCleanupExpiredRegistrations() {
        log.info("=== Nettoyage automatique des inscriptions expirées ===");
        try {
            cleanupExpiredRegistrations();
            log.info("✓ Nettoyage automatique terminé avec succès");
        } catch (Exception e) {
            log.error("✗ Erreur lors du nettoyage automatique des inscriptions expirées", e);
        }
    }
    
    /**
     * Supprime toutes les inscriptions en attente (non confirmées).
     * Méthode utilisée pour vider manuellement la liste des inscriptions en attente.
     * 
     * @return Le nombre d'inscriptions supprimées
     */
    @Transactional
    public long deleteAllPendingRegistrations() {
        long count = pendingRegistrationRepository.count();
        log.info("Suppression de toutes les inscriptions en attente ({} inscriptions)", count);
        pendingRegistrationRepository.deleteAll();
        log.info("✓ Toutes les inscriptions en attente ont été supprimées");
        return count;
    }

    /**
     * Récupère toutes les inscriptions en attente (non confirmées)
     * Inclut les inscriptions expirées pour que l'admin puisse les voir
     */
    public List<PendingRegistration> getAllPendingRegistrations() {
        return pendingRegistrationRepository.findAll().stream()
            .filter(pr -> pr.getConfirmed() == null || !pr.getConfirmed()) // Gérer les cas où confirmed pourrait être null
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Plus récentes en premier
            .collect(Collectors.toList());
    }
    
    /**
     * Supprime une inscription en attente spécifique par son ID
     * @param id ID de l'inscription en attente à supprimer
     * @throws IllegalArgumentException si l'inscription n'existe pas ou si l'ID est null
     */
    @Transactional
    public void deletePendingRegistration(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de l'inscription en attente ne peut pas être null");
        }
        Optional<PendingRegistration> pendingOpt = pendingRegistrationRepository.findById(id);
        if (pendingOpt.isEmpty()) {
            throw new IllegalArgumentException("Inscription en attente non trouvée avec l'ID: " + id);
        }
        pendingRegistrationRepository.deleteById(id);
        log.info("Inscription en attente supprimée: id={}", id);
    }
}

