package com.muhend.backend.auth.controller;

import com.muhend.backend.auth.dto.UserRegistrationRequest;
import com.muhend.backend.auth.service.PendingRegistrationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")  // Traefik enlève le préfixe /api avec stripprefix
@Slf4j
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final PendingRegistrationService pendingRegistrationService;

    public AuthController(PendingRegistrationService pendingRegistrationService) {
        this.pendingRegistrationService = pendingRegistrationService;
        logger.info("AuthController initialized");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest registrationRequest) {
        logger.info("=== Registration request received ===");
        logger.info("Username: {}", registrationRequest.getUsername());
        logger.info("Email: {}", registrationRequest.getEmail());
        logger.info("FirstName: {}", registrationRequest.getFirstName());
        logger.info("LastName: {}", registrationRequest.getLastName());
        logger.info("OrganizationName: {}", registrationRequest.getOrganizationName());
        logger.info("OrganizationEmail: {}", registrationRequest.getOrganizationEmail());

        try {
            // Créer une inscription en attente et envoyer l'email de confirmation
            var pending = pendingRegistrationService.createPendingRegistration(registrationRequest);
            
            logger.info("✓ Pending registration created: id={}, token={}", pending.getId(), pending.getConfirmationToken());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "Un email de confirmation a été envoyé à l'adresse " + registrationRequest.getOrganizationEmail() + 
                               ". Veuillez vérifier votre boîte de réception et cliquer sur le lien de confirmation pour finaliser votre inscription.",
                    "organizationEmail", registrationRequest.getOrganizationEmail()
                ));
        } catch (IllegalArgumentException e) {
            logger.error("✗ Error creating pending registration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("✗ Runtime error during user registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Erreur lors de la création de l'inscription",
                    "details", e.getMessage()
                ));
        }
    }
    
    @GetMapping("/confirm-registration")
    public ResponseEntity<?> confirmRegistration(@RequestParam String token) {
        logger.info("=== Registration confirmation request received ===");
        logger.info("Token: {}", token);
        
        try {
            pendingRegistrationService.confirmRegistration(token);
            
            logger.info("✓ Registration confirmed successfully: token={}", token);
            
            return ResponseEntity.ok(Map.of(
                "message", "Inscription confirmée avec succès ! Vous pouvez maintenant vous connecter.",
                "success", true
            ));
        } catch (IllegalArgumentException e) {
            logger.error("✗ Invalid confirmation token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            logger.error("✗ Registration state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("✗ Error confirming registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Erreur lors de la confirmation de l'inscription",
                    "details", e.getMessage()
                ));
        }
    }
}