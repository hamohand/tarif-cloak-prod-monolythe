package com.muhend.backend.invoice.controller;

import com.muhend.backend.invoice.dto.GenerateInvoiceRequest;
import com.muhend.backend.invoice.dto.InvoiceDto;
import com.muhend.backend.invoice.dto.UpdateInvoiceStatusRequest;
import com.muhend.backend.invoice.service.InvoicePdfService;
import com.muhend.backend.invoice.service.InvoiceService;
import com.muhend.backend.organization.exception.UserNotAssociatedException;
import com.muhend.backend.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour gérer les factures.
 * Phase 5 MVP : Facturation
 */
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoices", description = "Gestion des factures")
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
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
     * Récupère toutes les factures de l'utilisateur connecté (basées sur son organisation).
     */
    @GetMapping("/my-invoices")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer mes factures",
            description = "Retourne toutes les factures de l'organisation de l'utilisateur connecté. " +
                         "Un utilisateur doit toujours être associé à une organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<InvoiceDto>> getMyInvoices() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            List<InvoiceDto> invoices = invoiceService.getInvoicesByOrganization(organizationId);
            return ResponseEntity.ok(invoices);
        } catch (UserNotAssociatedException e) {
            log.error("Utilisateur {} non associé à une organisation", userId);
            // L'exception sera gérée par le GlobalExceptionHandler qui retournera un 403
            throw e;
        }
    }
    
    /**
     * Récupère une facture par son ID (pour l'utilisateur connecté).
     * Marque automatiquement la facture comme consultée.
     */
    @GetMapping("/my-invoices/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Récupérer une de mes factures",
            description = "Retourne une facture spécifique de l'organisation de l'utilisateur connecté et la marque comme consultée. " +
                         "Un utilisateur doit toujours être associé à une organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<InvoiceDto> getMyInvoice(@PathVariable Long id) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            
            InvoiceDto invoice = invoiceService.getInvoiceById(id);
            
            // Vérifier que la facture appartient à l'organisation de l'utilisateur
            if (!invoice.getOrganizationId().equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Marquer automatiquement comme consultée
            invoice = invoiceService.markInvoiceAsViewed(id);
            
            return ResponseEntity.ok(invoice);
        } catch (UserNotAssociatedException e) {
            log.error("Utilisateur {} non associé à une organisation", userId);
            throw e;
        }
    }
    
    /**
     * Compte les nouvelles factures non consultées de l'utilisateur connecté.
     */
    @GetMapping("/my-invoices/new-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Compter les nouvelles factures",
            description = "Retourne le nombre de nouvelles factures non consultées de l'organisation de l'utilisateur connecté. " +
                         "Un utilisateur doit toujours être associé à une organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> getNewInvoicesCount() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            long count = invoiceService.countNewInvoices(organizationId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (UserNotAssociatedException e) {
            log.error("Utilisateur {} non associé à une organisation", userId);
            throw e;
        }
    }
    
    /**
     * Compte les factures en retard (OVERDUE) de l'utilisateur connecté.
     */
    @GetMapping("/my-invoices/overdue-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Compter les factures en retard",
            description = "Retourne le nombre de factures en retard (OVERDUE) de l'organisation de l'utilisateur connecté. " +
                         "Un utilisateur doit toujours être associé à une organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> getOverdueInvoicesCount() {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            long count = invoiceService.countOverdueInvoices(organizationId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (UserNotAssociatedException e) {
            log.error("Utilisateur {} non associé à une organisation", userId);
            throw e;
        }
    }
    
    /**
     * Marque une facture comme consultée (pour l'utilisateur connecté).
     */
    @PutMapping("/my-invoices/{id}/mark-viewed")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Marquer une facture comme consultée",
            description = "Marque une facture comme consultée. Nécessite que la facture appartienne à l'organisation de l'utilisateur connecté. " +
                         "Un utilisateur doit toujours être associé à une organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<InvoiceDto> markInvoiceAsViewed(@PathVariable Long id) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            
            InvoiceDto invoice = invoiceService.getInvoiceById(id);
            
            // Vérifier que la facture appartient à l'organisation de l'utilisateur
            if (!invoice.getOrganizationId().equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            invoice = invoiceService.markInvoiceAsViewed(id);
            return ResponseEntity.ok(invoice);
        } catch (UserNotAssociatedException e) {
            log.error("Utilisateur {} non associé à une organisation", userId);
            throw e;
        }
    }
    
    /**
     * Récupère toutes les factures (admin uniquement).
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Récupérer toutes les factures",
            description = "Retourne toutes les factures de toutes les organisations. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<InvoiceDto>> getAllInvoices() {
        List<InvoiceDto> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }
    
    /**
     * Récupère les factures d'une organisation spécifique (admin uniquement).
     */
    @GetMapping("/admin/organization/{organizationId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Récupérer les factures d'une organisation",
            description = "Retourne toutes les factures d'une organisation. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<InvoiceDto>> getInvoicesByOrganization(@PathVariable Long organizationId) {
        List<InvoiceDto> invoices = invoiceService.getInvoicesByOrganization(organizationId);
        return ResponseEntity.ok(invoices);
    }
    
    /**
     * Récupère une facture par son ID (admin uniquement).
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Récupérer une facture",
            description = "Retourne une facture spécifique. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<InvoiceDto> getInvoice(@PathVariable Long id) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Génère une facture pour une période personnalisée (admin uniquement).
     */
    @PostMapping("/admin/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Générer une facture",
            description = "Génère une facture pour une organisation et une période spécifiques. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<InvoiceDto> generateInvoice(@Valid @RequestBody GenerateInvoiceRequest request) {
        InvoiceDto invoice = invoiceService.generateInvoice(
                request.getOrganizationId(),
                request.getPeriodStart(),
                request.getPeriodEnd()
        );
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Génère une facture mensuelle pour une organisation (admin uniquement).
     */
    @PostMapping("/admin/generate-monthly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Générer une facture mensuelle",
            description = "Génère une facture mensuelle pour une organisation. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<InvoiceDto> generateMonthlyInvoice(
            @RequestParam Long organizationId,
            @RequestParam int year,
            @RequestParam int month) {
        InvoiceDto invoice = invoiceService.generateMonthlyInvoice(organizationId, year, month);
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Génère les factures mensuelles pour toutes les organisations (admin uniquement).
     */
    @PostMapping("/admin/generate-all-monthly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Générer les factures mensuelles pour toutes les organisations",
            description = "Génère les factures mensuelles pour toutes les organisations ayant une utilisation. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Object>> generateAllMonthlyInvoices(
            @RequestParam int year,
            @RequestParam int month) {
        List<InvoiceDto> invoices = invoiceService.generateMonthlyInvoicesForAllOrganizations(year, month);
        return ResponseEntity.ok(Map.of(
                "message", "Factures générées avec succès",
                "count", invoices.size(),
                "invoices", invoices
        ));
    }
    
    /**
     * Met à jour le statut d'une facture (admin uniquement).
     */
    @PutMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Mettre à jour le statut d'une facture",
            description = "Met à jour le statut d'une facture (ex: marquer comme payée). Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<InvoiceDto> updateInvoiceStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceStatusRequest request) {
        InvoiceDto invoice = invoiceService.updateInvoiceStatus(id, request.getStatus(), request.getNotes());
        return ResponseEntity.ok(invoice);
    }
    
    /**
     * Télécharge le PDF d'une facture (pour l'utilisateur connecté).
     */
    @GetMapping("/my-invoices/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Télécharger le PDF d'une de mes factures",
            description = "Télécharge le PDF d'une facture de l'organisation de l'utilisateur connecté. " +
                         "Un utilisateur doit toujours être associé à une organisation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<byte[]> downloadMyInvoicePdf(@PathVariable Long id) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Long organizationId = organizationService.getOrganizationIdByUserId(userId);
            
            InvoiceDto invoice = invoiceService.getInvoiceById(id);
            
            // Vérifier que la facture appartient à l'organisation de l'utilisateur
            if (!invoice.getOrganizationId().equals(organizationId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return generatePdfResponse(invoice);
        } catch (UserNotAssociatedException e) {
            log.error("Utilisateur {} non associé à une organisation", userId);
            throw e;
        }
    }
    
    /**
     * Télécharge le PDF d'une facture (admin uniquement).
     */
    @GetMapping("/admin/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Télécharger le PDF d'une facture",
            description = "Télécharge le PDF d'une facture. Nécessite le rôle ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        return generatePdfResponse(invoice);
    }
    
    /**
     * Génère une réponse PDF à partir d'une facture.
     */
    private ResponseEntity<byte[]> generatePdfResponse(InvoiceDto invoice) {
        try {
            byte[] pdfBytes = invoicePdfService.generatePdf(invoice);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                    "facture_" + invoice.getInvoiceNumber() + ".pdf");
            headers.setContentLength(pdfBytes.length);
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Erreur lors de la génération du PDF pour la facture {}", invoice.getInvoiceNumber(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

