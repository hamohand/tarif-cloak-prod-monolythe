package com.muhend.backend.payment.controller;

import com.muhend.backend.payment.dto.PaymentDto;
import com.muhend.backend.payment.dto.SubscriptionDto;
import com.muhend.backend.payment.service.PaymentService;
import com.muhend.backend.payment.service.StripeSyncService;
import com.muhend.backend.payment.service.SubscriptionService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller admin pour gérer les paiements et abonnements.
 */
@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Payments", description = "Gestion admin des paiements et abonnements")
public class AdminPaymentController {
    
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final StripeSyncService stripeSyncService;
    
    /**
     * Récupère tous les paiements (admin uniquement).
     */
    @GetMapping
    @Operation(
            summary = "Récupérer tous les paiements",
            description = "Retourne tous les paiements de toutes les organisations. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<PaymentDto>> getAllPayments() {
        List<PaymentDto> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Récupère tous les paiements d'une organisation (admin uniquement).
     */
    @GetMapping("/organization/{organizationId}")
    @Operation(
            summary = "Récupérer les paiements d'une organisation",
            description = "Retourne tous les paiements d'une organisation spécifique. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<PaymentDto>> getPaymentsByOrganization(@PathVariable Long organizationId) {
        List<PaymentDto> payments = paymentService.getPaymentsByOrganization(organizationId);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Récupère un paiement par son ID (admin uniquement).
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Récupérer un paiement",
            description = "Retourne un paiement spécifique. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PaymentDto> getPayment(@PathVariable Long id) {
        PaymentDto payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Récupère tous les abonnements (admin uniquement).
     */
    @GetMapping("/subscriptions")
    @Operation(
            summary = "Récupérer tous les abonnements",
            description = "Retourne tous les abonnements de toutes les organisations. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<SubscriptionDto>> getAllSubscriptions() {
        List<SubscriptionDto> subscriptions = subscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(subscriptions);
    }
    
    /**
     * Récupère tous les abonnements d'une organisation (admin uniquement).
     */
    @GetMapping("/subscriptions/organization/{organizationId}")
    @Operation(
            summary = "Récupérer les abonnements d'une organisation",
            description = "Retourne tous les abonnements d'une organisation spécifique. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<SubscriptionDto>> getSubscriptionsByOrganization(@PathVariable Long organizationId) {
        List<SubscriptionDto> subscriptions = subscriptionService.getSubscriptionsByOrganization(organizationId);
        return ResponseEntity.ok(subscriptions);
    }
    
    /**
     * Récupère un abonnement par son ID (admin uniquement).
     */
    @GetMapping("/subscriptions/{id}")
    @Operation(
            summary = "Récupérer un abonnement",
            description = "Retourne un abonnement spécifique. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SubscriptionDto> getSubscription(@PathVariable Long id) {
        SubscriptionDto subscription = subscriptionService.getSubscriptionById(id);
        return ResponseEntity.ok(subscription);
    }
    
    /**
     * Synchronise tous les abonnements Stripe avec la base de données locale (admin uniquement).
     */
    @PostMapping("/sync/subscriptions")
    @Operation(
            summary = "Synchroniser tous les abonnements Stripe",
            description = "Récupère tous les abonnements depuis Stripe et les synchronise avec la base de données locale. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> syncAllSubscriptions() {
        try {
            int syncedCount = stripeSyncService.syncAllSubscriptions();
            return ResponseEntity.ok(Map.of(
                    "message", "Synchronisation terminée",
                    "syncedCount", syncedCount
            ));
        } catch (StripeException e) {
            log.error("Erreur lors de la synchronisation des abonnements Stripe", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur lors de la synchronisation",
                    "message", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Stripe n'est pas configuré",
                    "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Synchronise les abonnements d'une organisation spécifique (admin uniquement).
     */
    @PostMapping("/sync/organization/{organizationId}/subscriptions")
    @Operation(
            summary = "Synchroniser les abonnements d'une organisation",
            description = "Récupère les abonnements d'une organisation depuis Stripe et les synchronise avec la base de données locale. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> syncOrganizationSubscriptions(@PathVariable Long organizationId) {
        try {
            int syncedCount = stripeSyncService.syncOrganizationSubscriptions(organizationId);
            return ResponseEntity.ok(Map.of(
                    "message", "Synchronisation terminée",
                    "organizationId", organizationId,
                    "syncedCount", syncedCount
            ));
        } catch (StripeException e) {
            log.error("Erreur lors de la synchronisation des abonnements pour l'organisation {}", organizationId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Erreur lors de la synchronisation",
                    "message", e.getMessage()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Erreur de validation",
                    "message", e.getMessage()
            ));
        }
    }
}
