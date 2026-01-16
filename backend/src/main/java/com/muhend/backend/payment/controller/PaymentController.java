package com.muhend.backend.payment.controller;

import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.payment.dto.CheckoutSessionResponse;
import com.muhend.backend.payment.dto.CreateCheckoutSessionRequest;
import com.muhend.backend.payment.dto.PayInvoiceRequest;
import com.muhend.backend.payment.dto.PaymentDto;
import com.muhend.backend.payment.service.PaymentService;
import com.muhend.backend.payment.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller pour gérer les paiements.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Gestion des paiements")
public class PaymentController {
    
    private final StripeService stripeService;
    private final PaymentService paymentService;
    private final OrganizationService organizationService;
    
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
     * Crée une session de checkout Stripe pour un plan tarifaire.
     */
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Créer une session de checkout",
            description = "Crée une session de checkout Stripe pour souscrire à un plan tarifaire.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            if (organizationId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            CheckoutSessionResponse response = stripeService.createCheckoutSession(organizationId, request);
            return ResponseEntity.ok(response);
            
        } catch (StripeException e) {
            log.error("Erreur lors de la création de la session de checkout Stripe", e);
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException e) {
            log.error("Erreur de validation lors de la création de la session de checkout", e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.error("Stripe n'est pas configuré", e);
            return ResponseEntity.status(503).build(); // Service Unavailable
        }
    }
    
    /**
     * Récupère tous les paiements de l'organisation de l'utilisateur connecté.
     */
    @GetMapping("/my-payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer mes paiements",
            description = "Retourne tous les paiements de l'organisation de l'utilisateur connecté.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<PaymentDto>> getMyPayments() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.ok(List.of());
        }
        
        List<PaymentDto> payments = paymentService.getPaymentsByOrganization(organizationId);
        return ResponseEntity.ok(payments);
    }
    
    /**
     * Récupère un paiement par son ID (pour l'utilisateur connecté).
     */
    @GetMapping("/my-payments/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer un de mes paiements",
            description = "Retourne un paiement spécifique de l'organisation de l'utilisateur connecté.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<PaymentDto> getMyPayment(@PathVariable Long id) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long organizationId = organizationService.getOrganizationIdByUserId(userId);
        if (organizationId == null) {
            return ResponseEntity.notFound().build();
        }
        
        PaymentDto payment = paymentService.getPaymentById(id);
        
        // Vérifier que le paiement appartient à l'organisation de l'utilisateur
        if (!payment.getOrganizationId().equals(organizationId)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Récupère le statut d'une session de checkout.
     */
    @GetMapping("/checkout-session/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer le statut d'une session de checkout",
            description = "Récupère les informations d'une session de checkout Stripe.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> getCheckoutSessionStatus(@PathVariable String sessionId) {
        try {
            Session session = stripeService.getCheckoutSession(sessionId);
            
            Map<String, Object> response = Map.of(
                    "id", session.getId(),
                    "status", session.getStatus(),
                    "paymentStatus", session.getPaymentStatus(),
                    "customerId", session.getCustomer() != null ? session.getCustomer() : "",
                    "subscriptionId", session.getSubscription() != null ? session.getSubscription() : ""
            );
            
            return ResponseEntity.ok(response);
            
        } catch (StripeException e) {
            log.error("Erreur lors de la récupération de la session de checkout", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Crée une session de checkout pour payer une facture.
     */
    @PostMapping("/invoices/{invoiceId}/checkout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Payer une facture",
            description = "Crée une session de checkout Stripe pour payer une facture spécifique.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<CheckoutSessionResponse> payInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody(required = false) PayInvoiceRequest request) {
        try {
            String userId = getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            if (organizationId == null) {
                return ResponseEntity.badRequest().build();
            }
            
            String successUrl = request != null && request.getSuccessUrl() != null ? 
                    request.getSuccessUrl() : null;
            String cancelUrl = request != null && request.getCancelUrl() != null ? 
                    request.getCancelUrl() : null;
            
            CheckoutSessionResponse response = stripeService.createInvoiceCheckoutSession(
                    organizationId, invoiceId, successUrl, cancelUrl);
            return ResponseEntity.ok(response);
            
        } catch (StripeException e) {
            log.error("Erreur lors de la création de la session de checkout pour la facture", e);
            return ResponseEntity.internalServerError().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Erreur de validation lors du paiement de la facture", e);
            return ResponseEntity.badRequest().build();
        }
    }
}

