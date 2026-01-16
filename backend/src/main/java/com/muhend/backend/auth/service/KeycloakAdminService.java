package com.muhend.backend.auth.service;

import com.muhend.backend.auth.dto.UserRegistrationRequest;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final Keycloak keycloak;

    @Value("${keycloak.registration.realm}")
    private String realm;

    public KeycloakAdminService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public Response createUser(UserRegistrationRequest registrationRequest) {
        return createUser(
                registrationRequest.getUsername(),
                registrationRequest.getEmail(),
                registrationRequest.getPassword(),
                registrationRequest.getFirstName(),
                registrationRequest.getLastName(),
                true,
                false,
                null,
                null
        );
    }

    public Response createUser(String username,
                               String email,
                               String password,
                               String firstName,
                               String lastName,
                               boolean enabled,
                               boolean emailVerified,
                               List<String> requiredActions,
                               Map<String, List<String>> attributes) {
        logger.info("Creating user in Keycloak realm: {}", realm);
        logger.info("Username: {}, Email: {}", username, email);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            try {
                UserRepresentation user = new UserRepresentation();
                user.setEnabled(enabled);
                user.setUsername(username);
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                user.setEmailVerified(emailVerified);
                if (requiredActions != null && !requiredActions.isEmpty()) {
                    user.setRequiredActions(requiredActions);
                }
                if (attributes != null && !attributes.isEmpty()) {
                    user.setAttributes(attributes);
                }

                CredentialRepresentation passwordCred = new CredentialRepresentation();
                passwordCred.setTemporary(false);
                passwordCred.setType(CredentialRepresentation.PASSWORD);
                passwordCred.setValue(password);
                user.setCredentials(Collections.singletonList(passwordCred));

                RealmResource realmResource = keycloak.realm(realm);
                UsersResource usersResource = realmResource.users();

                logger.info("Sending create user request to Keycloak (attempt {}/{})", attempt + 1, MAX_RETRIES);
                Response response = usersResource.create(user);

                int status = response.getStatus();
                logger.info("Keycloak response status: {}", status);

                if (status >= 200 && status < 300) {
                    logger.info("✓ User created successfully");
                    return response;
                } else if (status == 409) {
                    logger.warn("User already exists");
                    return response;
                } else {
                    String errorBody = response.hasEntity() ? response.readEntity(String.class) : "No error body";
                    logger.error("Failed to create user. Status: {}, Body: {}", status, errorBody);
                    return response;
                }

            } catch (Exception e) {
                lastException = e;
                attempt++;
                logger.warn("Attempt {}/{} failed to connect to Keycloak: {}",
                    attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        logger.info("Retrying in {} ms...", RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while retrying Keycloak connection", ie);
                    }
                }
            }
        }

        logger.error("Failed to create user after {} attempts", MAX_RETRIES);
        throw new RuntimeException("Failed to connect to Keycloak after " + MAX_RETRIES + " attempts", lastException);
    }
    
    /**
     * Extrait l'ID Keycloak d'un utilisateur depuis l'en-tête Location de la réponse.
     *
     * @param response La réponse Keycloak après création d'un utilisateur
     * @return L'ID Keycloak de l'utilisateur ou null si non trouvé
     */
    public String getUserIdFromResponse(Response response) {
        try {
            String location = response.getLocation() != null ? response.getLocation().toString() : null;
            if (location != null) {
                // L'URL est généralement au format: .../users/{userId}
                int lastSlashIndex = location.lastIndexOf('/');
                if (lastSlashIndex >= 0 && lastSlashIndex < location.length() - 1) {
                    String userId = location.substring(lastSlashIndex + 1);
                    logger.info("User ID extracted from Location header: {}", userId);
                    return userId;
                }
            }
            logger.warn("Location header not found or invalid in response");
            return null;
        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction de l'ID utilisateur depuis la réponse: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère l'ID Keycloak d'un utilisateur par son nom d'utilisateur.
     * Méthode de fallback si l'extraction depuis la réponse échoue.
     *
     * @param username Le nom d'utilisateur
     * @return L'ID Keycloak de l'utilisateur ou null si non trouvé
     */
    public String getUserIdByUsername(String username) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            java.util.List<org.keycloak.representations.idm.UserRepresentation> users = 
                realmResource.users().searchByUsername(username, true);
            
            if (users != null && !users.isEmpty()) {
                // Chercher l'utilisateur exact (car searchByUsername peut retourner plusieurs résultats)
                for (org.keycloak.representations.idm.UserRepresentation user : users) {
                    if (username.equals(user.getUsername())) {
                        return user.getId();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'ID utilisateur pour '{}': {}", username, e.getMessage());
            return null;
        }
    }

    /**
     * Récupère l'email d'un utilisateur depuis Keycloak.
     *
     * @param keycloakUserId ID de l'utilisateur Keycloak
     * @return L'email de l'utilisateur ou null si non trouvé
     */
    public String getUserEmail(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserRepresentation user = realmResource.users().get(keycloakUserId).toRepresentation();
            return user.getEmail();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'email de l'utilisateur {}: {}", keycloakUserId, e.getMessage());
            return null;
        }
    }

    /**
     * Récupère les emails de plusieurs utilisateurs depuis Keycloak.
     *
     * @param keycloakUserIds Liste des IDs d'utilisateurs Keycloak
     * @return Liste des emails (sans les nulls)
     */
    public java.util.List<String> getUserEmails(java.util.List<String> keycloakUserIds) {
        return keycloakUserIds.stream()
                .map(this::getUserEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Récupère le nom d'utilisateur (username) d'un utilisateur depuis Keycloak.
     *
     * @param keycloakUserId ID de l'utilisateur Keycloak
     * @return Le nom d'utilisateur ou null si non trouvé
     */
    public String getUsername(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserRepresentation user = realmResource.users().get(keycloakUserId).toRepresentation();
            return user.getUsername();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du nom d'utilisateur pour {}: {}", keycloakUserId, e.getMessage());
            return null;
        }
    }

    public void disableUser(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserRepresentation user = usersResource.get(keycloakUserId).toRepresentation();
            user.setEnabled(false);
            usersResource.get(keycloakUserId).update(user);
            logger.info("Utilisateur {} désactivé dans Keycloak", keycloakUserId);
        } catch (Exception e) {
            logger.error("Erreur lors de la désactivation de l'utilisateur {}: {}", keycloakUserId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la désactivation de l'utilisateur: " + e.getMessage(), e);
        }
    }

    /**
     * Active un utilisateur dans Keycloak.
     *
     * @param keycloakUserId ID de l'utilisateur Keycloak
     */
    public void enableUser(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserRepresentation user = usersResource.get(keycloakUserId).toRepresentation();
            user.setEnabled(true);
            usersResource.get(keycloakUserId).update(user);
            logger.info("Utilisateur {} activé dans Keycloak", keycloakUserId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'activation de l'utilisateur {}: {}", keycloakUserId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'activation de l'utilisateur: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie si un utilisateur est activé dans Keycloak.
     *
     * @param keycloakUserId ID de l'utilisateur Keycloak
     * @return true si l'utilisateur est activé, false sinon
     */
    public boolean isUserEnabled(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserRepresentation user = realmResource.users().get(keycloakUserId).toRepresentation();
            return user.isEnabled() != null && user.isEnabled();
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification du statut de l'utilisateur {}: {}", keycloakUserId, e.getMessage(), e);
            return false;
        }
    }

    public UserRepresentation getUserRepresentation(String keycloakUserId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            return realmResource.users().get(keycloakUserId).toRepresentation();
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'utilisateur {}: {}", keycloakUserId, e.getMessage());
            return null;
        }
    }

    public void assignRealmRoles(String keycloakUserId, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }
        try {
            RealmResource realmResource = keycloak.realm(realm);
            RolesResource rolesResource = realmResource.roles();

            List<RoleRepresentation> roles = roleNames.stream()
                    .map(roleName -> {
                        try {
                            return rolesResource.get(roleName).toRepresentation();
                        } catch (Exception e) {
                            logger.warn("Rôle {} introuvable dans le realm {}", roleName, realm);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (roles.isEmpty()) {
                logger.warn("Aucun rôle valide à assigner pour l'utilisateur {}", keycloakUserId);
                return;
            }

            realmResource.users().get(keycloakUserId).roles().realmLevel().add(roles);
            logger.info("Rôles {} assignés à l'utilisateur {}", roles.stream().map(RoleRepresentation::getName).toList(), keycloakUserId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'assignation des rôles {} à l'utilisateur {}: {}", roleNames, keycloakUserId, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'assignation des rôles: " + e.getMessage(), e);
        }
    }
}