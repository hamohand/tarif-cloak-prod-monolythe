package com.muhend.backend.pricing.controller;

import com.muhend.backend.organization.dto.OrganizationDto;
import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.pricing.dto.CreateQuoteRequestDto;
import com.muhend.backend.pricing.dto.QuoteRequestDto;
import com.muhend.backend.pricing.dto.UpdateQuoteRequestDto;
import com.muhend.backend.pricing.model.QuoteRequest;
import com.muhend.backend.pricing.service.QuoteRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller pour gérer les demandes de devis.
 */
@RestController
@RequestMapping("/quote-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quote Requests", description = "Endpoints pour gérer les demandes de devis personnalisés")
public class QuoteRequestController {
    
    private final QuoteRequestService quoteRequestService;
    private final OrganizationService organizationService;
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Créer une demande de devis",
        description = "Permet à une organisation de demander un devis pour un plan tarifaire personnalisé. L'organisation est récupérée automatiquement depuis le token d'authentification.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> createQuoteRequest(@Valid @RequestBody CreateQuoteRequestDto dto) {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            // Créer un nouveau DTO avec l'organizationId récupéré depuis le token
            CreateQuoteRequestDto dtoWithOrgId = new CreateQuoteRequestDto();
            dtoWithOrgId.setOrganizationId(organization.getId());
            dtoWithOrgId.setContactName(dto.getContactName());
            dtoWithOrgId.setContactEmail(dto.getContactEmail());
            dtoWithOrgId.setMessage(dto.getMessage());
            
            QuoteRequestDto created = quoteRequestService.createQuoteRequest(dtoWithOrgId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Erreur lors de la création de la demande de devis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Erreur lors de la création de la demande de devis"));
        }
    }
    
    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Récupérer les demandes de devis d'une organisation",
        description = "Retourne toutes les demandes de devis d'une organisation spécifique. L'utilisateur doit être membre de cette organisation.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getQuoteRequestsByOrganization(@PathVariable Long organizationId) {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        
        try {
            // Vérifier que l'utilisateur appartient à cette organisation
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            if (!organization.getId().equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "FORBIDDEN", "message", "Vous n'avez pas accès à cette organisation"));
            }
            
            List<QuoteRequestDto> requests = quoteRequestService.getQuoteRequestsByOrganization(organizationId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des demandes de devis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Erreur lors de la récupération des demandes de devis"));
        }
    }
    
    @GetMapping("/my-organization")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Récupérer mes demandes de devis",
        description = "Retourne toutes les demandes de devis de l'organisation de l'utilisateur connecté.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getMyOrganizationQuoteRequests() {
        String organizationUserId = getCurrentUserId();
        if (organizationUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "AUTH_REQUIRED", "message", "Authentification requise"));
        }
        
        try {
            OrganizationDto organization = organizationService.getOrganizationByKeycloakUserId(organizationUserId);
            List<QuoteRequestDto> requests = quoteRequestService.getQuoteRequestsByOrganization(organization.getId());
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des demandes de devis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Erreur lors de la récupération des demandes de devis"));
        }
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Récupérer toutes les demandes de devis",
        description = "Retourne toutes les demandes de devis. Réservé aux administrateurs.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<QuoteRequestDto>> getAllQuoteRequests() {
        List<QuoteRequestDto> requests = quoteRequestService.getAllQuoteRequests();
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Récupérer une demande de devis par ID",
        description = "Retourne une demande de devis spécifique. Réservé aux administrateurs.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getQuoteRequestById(@PathVariable Long id) {
        try {
            QuoteRequestDto request = quoteRequestService.getQuoteRequestById(id);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException e) {
            log.error("Demande de devis non trouvée: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Mettre à jour une demande de devis",
        description = "Met à jour le statut et/ou les notes d'une demande de devis. Réservé aux administrateurs.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> updateQuoteRequest(@PathVariable Long id, @Valid @RequestBody UpdateQuoteRequestDto dto) {
        try {
            QuoteRequestDto updated = quoteRequestService.updateQuoteRequest(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Erreur lors de la mise à jour de la demande de devis: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la demande de devis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Erreur lors de la mise à jour de la demande de devis"));
        }
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Récupérer les demandes de devis par statut",
        description = "Retourne les demandes de devis filtrées par statut. Réservé aux administrateurs.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<QuoteRequestDto>> getQuoteRequestsByStatus(@PathVariable String status) {
        try {
            QuoteRequest.QuoteStatus quoteStatus = QuoteRequest.QuoteStatus.valueOf(status.toUpperCase());
            List<QuoteRequestDto> requests = quoteRequestService.getQuoteRequestsByStatus(quoteStatus);
            return ResponseEntity.ok(requests);
        } catch (IllegalArgumentException e) {
            log.error("Statut invalide: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }
    
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("sub");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'ID utilisateur courant", e);
        }
        return null;
    }
}

