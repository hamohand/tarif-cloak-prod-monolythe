package com.muhend.backend.payment.controller;

import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.payment.dto.CancelSubscriptionRequest;
import com.muhend.backend.payment.dto.SubscriptionDto;
import com.muhend.backend.payment.service.SubscriptionService;
import com.muhend.backend.payment.service.StripeService;
import com.stripe.exception.StripeException;
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

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller pour gérer les abonnements.
 */
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Gestion des abonnements")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final OrganizationService organizationService;
    private final StripeService stripeService;
    
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
    
    /**
     * Récupère tous les abonnements de l'organisation de l'utilisateur connecté.
     */
    @GetMapping("/my-subscriptions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer mes abonnements",
            description = "Retourne tous les abonnements de l'organisation de l'utilisateur connecté.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<SubscriptionDto>> getMySubscriptions() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.ok(List.of());
        }
        
        List<SubscriptionDto> subscriptions = subscriptionService.getSubscriptionsByOrganization(organizationId);
        return ResponseEntity.ok(subscriptions);
    }
    
    /**
     * Récupère l'abonnement actif de l'organisation de l'utilisateur connecté.
     */
    @GetMapping("/my-subscriptions/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer mon abonnement actif",
            description = "Retourne l'abonnement actif de l'organisation de l'utilisateur connecté.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SubscriptionDto> getMyActiveSubscription() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.notFound().build();
        }
        
        SubscriptionDto subscription = subscriptionService.getActiveSubscription(organizationId);
        if (subscription == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Récupère un abonnement par son ID (pour l'utilisateur connecté).
     */
    @GetMapping("/my-subscriptions/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer un de mes abonnements",
            description = "Retourne un abonnement spécifique de l'organisation de l'utilisateur connecté.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SubscriptionDto> getMySubscription(@PathVariable Long id) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.notFound().build();
        }
        
        SubscriptionDto subscription = subscriptionService.getSubscriptionById(id);
        
        // Vérifier que l'abonnement appartient à l'organisation de l'utilisateur
        if (!subscription.getOrganizationId().equals(organizationId)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Annule un abonnement.
     */
    @PostMapping("/my-subscriptions/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Annuler un abonnement",
            description = "Annule un abonnement de l'organisation de l'utilisateur connecté. " +
                         "L'abonnement peut être annulé immédiatement ou à la fin de la période actuelle.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SubscriptionDto> cancelSubscription(
            @PathVariable Long id,
            @Valid @RequestBody CancelSubscriptionRequest request) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            if (organizationId == null) {
                return ResponseEntity.notFound().build();
            }
            
            SubscriptionDto subscription = subscriptionService.getSubscriptionById(id);
            
            // Vérifier que l'abonnement appartient à l'organisation de l'utilisateur
            if (!subscription.getOrganizationId().equals(organizationId)) {
                return ResponseEntity.status(403).build();
            }
            
            // Annuler l'abonnement via Stripe
            boolean cancelAtPeriodEnd = request.getCancelAtPeriodEnd() != null ? request.getCancelAtPeriodEnd() : true;
            stripeService.cancelSubscription(id, cancelAtPeriodEnd);
            
            // Récupérer l'abonnement mis à jour
            subscription = subscriptionService.getSubscriptionById(id);
            
            return ResponseEntity.ok(subscription);
            
        } catch (StripeException e) {
            log.error("Erreur lors de l'annulation de l'abonnement Stripe", e);
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Erreur lors de l'annulation de l'abonnement", e);
            return ResponseEntity.badRequest().build();
        }
    }
}

