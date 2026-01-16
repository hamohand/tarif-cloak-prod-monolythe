package com.muhend.backend.invoice.service;

import com.muhend.backend.invoice.dto.InvoiceDto;
import com.muhend.backend.invoice.dto.InvoiceItemDto;
import com.muhend.backend.invoice.model.Invoice;
import com.muhend.backend.invoice.model.InvoiceItem;
import com.muhend.backend.invoice.repository.InvoiceItemRepository;
import com.muhend.backend.invoice.repository.InvoiceRepository;
import com.muhend.backend.email.service.EmailService;
import com.muhend.backend.auth.service.KeycloakAdminService;
import com.muhend.backend.organization.dto.OrganizationDto;
import com.muhend.backend.organization.dto.OrganizationUserDto;
import com.muhend.backend.organization.service.OrganizationService;
import com.muhend.backend.pricing.dto.PricingPlanDto;
import com.muhend.backend.usage.model.UsageLog;
import com.muhend.backend.usage.repository.UsageLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour gérer les factures.
 * Phase 5 MVP : Facturation
 */
@Service
@Slf4j
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final UsageLogRepository usageLogRepository;
    private final OrganizationService organizationService;
    private final EmailService emailService;
    private final KeycloakAdminService keycloakAdminService;
    
    private static final DateTimeFormatter INVOICE_NUMBER_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            UsageLogRepository usageLogRepository,
            @Lazy OrganizationService organizationService,
            EmailService emailService,
            KeycloakAdminService keycloakAdminService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.usageLogRepository = usageLogRepository;
        this.organizationService = organizationService;
        this.emailService = emailService;
        this.keycloakAdminService = keycloakAdminService;
    }
    
    /**
     * Génère une facture mensuelle pour une organisation.
     * 
     * @param organizationId ID de l'organisation
     * @param year Année
     * @param month Mois (1-12)
     * @return La facture générée
     */
    @Transactional
    public InvoiceDto generateMonthlyInvoice(Long organizationId, int year, int month) {
        // Vérifier que l'organisation existe
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId);
        }
        
        // Calculer les dates de la période
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        
        // Vérifier si une facture existe déjà pour cette période
        if (invoiceRepository.existsByOrganizationIdAndPeriodStartAndPeriodEnd(
                organizationId, periodStart, periodEnd)) {
            throw new IllegalArgumentException(
                    String.format("Une facture existe déjà pour l'organisation %d pour la période %s",
                            organizationId, yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        }
        
        // Récupérer les logs d'utilisation pour la période
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(LocalTime.MAX);
        
        List<UsageLog> usageLogs = usageLogRepository.findByOrganizationIdAndTimestampBetween(
                organizationId, startDateTime, endDateTime);
        
        if (usageLogs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucune utilisation trouvée pour cette période. Impossible de générer une facture.");
        }
        
        // Calculer le total
        BigDecimal totalAmount = usageLogs.stream()
                .filter(log -> log.getCostUsd() != null)
                .map(UsageLog::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Générer le numéro de facture
        String invoiceNumber = generateInvoiceNumber(organizationId, year, month);
        
        // Créer la facture
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrganizationName(organization.getName());
        invoice.setOrganizationEmail(organization.getEmail());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(periodEnd.plusDays(30)); // 30 jours après la fin de la période
        
        invoice = invoiceRepository.save(invoice);
        
        // Créer les lignes de facture
        List<InvoiceItem> items = createInvoiceItems(invoice, usageLogs);
        if (!items.isEmpty()) {
            invoiceItemRepository.saveAll(items);
        }
        
        log.info("Facture générée: {} pour l'organisation {} (période: {})",
                invoiceNumber, organization.getName(), yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        
        InvoiceDto invoiceDto = toDto(invoice);
        
        // Envoyer un email de notification
        sendInvoiceNotificationEmail(invoiceDto, organization);
        
        return invoiceDto;
    }
    
    /**
     * Génère une facture pour une période personnalisée.
     * 
     * @param organizationId ID de l'organisation
     * @param periodStart Date de début de la période
     * @param periodEnd Date de fin de la période
     * @return La facture générée
     */
    @Transactional
    public InvoiceDto generateInvoice(Long organizationId, LocalDate periodStart, LocalDate periodEnd) {
        // Vérifier que l'organisation existe
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId);
        }
        
        // Vérifier si une facture existe déjà pour cette période
        if (invoiceRepository.existsByOrganizationIdAndPeriodStartAndPeriodEnd(
                organizationId, periodStart, periodEnd)) {
            throw new IllegalArgumentException(
                    String.format("Une facture existe déjà pour l'organisation %d pour la période %s - %s",
                            organizationId, periodStart, periodEnd));
        }
        
        // Récupérer les logs d'utilisation pour la période
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(LocalTime.MAX);
        
        List<UsageLog> usageLogs = usageLogRepository.findByOrganizationIdAndTimestampBetween(
                organizationId, startDateTime, endDateTime);
        
        if (usageLogs.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucune utilisation trouvée pour cette période. Impossible de générer une facture.");
        }
        
        // Calculer le total
        BigDecimal totalAmount = usageLogs.stream()
                .filter(log -> log.getCostUsd() != null)
                .map(UsageLog::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Générer le numéro de facture (basé sur la date de début)
        YearMonth yearMonth = YearMonth.from(periodStart);
        String invoiceNumber = generateInvoiceNumber(organizationId, yearMonth.getYear(), yearMonth.getMonthValue());
        
        // Créer la facture
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrganizationName(organization.getName());
        invoice.setOrganizationEmail(organization.getEmail());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(periodEnd.plusDays(30)); // 30 jours après la fin de la période
        
        invoice = invoiceRepository.save(invoice);
        
        // Créer les lignes de facture
        List<InvoiceItem> items = createInvoiceItems(invoice, usageLogs);
        if (!items.isEmpty()) {
            invoiceItemRepository.saveAll(items);
        }
        
        log.info("Facture générée: {} pour l'organisation {} (période: {} - {})",
                invoiceNumber, organization.getName(), periodStart, periodEnd);
        
        InvoiceDto invoiceDto = toDto(invoice);
        
        // Envoyer un email de notification
        sendInvoiceNotificationEmail(invoiceDto, organization);
        
        return invoiceDto;
    }
    
    /**
     * Génère une facture bihebdomadaire (14 jours) pour une organisation avec un plan Pay-per-Request.
     * 
     * @param organizationId ID de l'organisation
     * @param periodStart Date de début de la période (14 jours avant aujourd'hui)
     * @param periodEnd Date de fin de la période (hier)
     * @return La facture générée, ou null si aucune facture n'a été générée (déjà existante ou aucune utilisation)
     */
    @Transactional
    public InvoiceDto generateBiweeklyInvoice(Long organizationId, LocalDate periodStart, LocalDate periodEnd) {
        // Vérifier que l'organisation existe
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId);
        }
        
        // Vérifier si une facture existe déjà pour cette période
        if (invoiceRepository.existsByOrganizationIdAndPeriodStartAndPeriodEnd(
                organizationId, periodStart, periodEnd)) {
            log.info("Une facture existe déjà pour l'organisation {} pour la période {} - {}. Facture non générée.",
                    organizationId, periodStart, periodEnd);
            return null; // Retourner null au lieu de lever une exception pour permettre le scheduler de continuer
        }
        
        // Récupérer les logs d'utilisation pour la période
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(LocalTime.MAX);
        
        List<UsageLog> usageLogs = usageLogRepository.findByOrganizationIdAndTimestampBetween(
                organizationId, startDateTime, endDateTime);
        
        // Si aucune utilisation, ne pas générer de facture
        if (usageLogs.isEmpty()) {
            log.info("Aucune utilisation trouvée pour l'organisation {} sur la période {} - {}. Facture non générée.",
                    organizationId, periodStart, periodEnd);
            return null;
        }
        
        // Calculer le total
        BigDecimal totalAmount = usageLogs.stream()
                .filter(log -> log.getCostUsd() != null)
                .map(UsageLog::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Générer le numéro de facture (format: ORG-YYYYMMDD-BIWEEKLY)
        String invoiceNumber = String.format("ORG-%d-%s-BIWEEKLY",
                organizationId,
                periodStart.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        // Créer la facture
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrganizationName(organization.getName());
        invoice.setOrganizationEmail(organization.getEmail());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(periodEnd.plusDays(14)); // Échéance dans 14 jours
        
        invoice = invoiceRepository.save(invoice);
        
        // Créer les lignes de facture
        List<InvoiceItem> items = createInvoiceItems(invoice, usageLogs);
        if (!items.isEmpty()) {
            invoiceItemRepository.saveAll(items);
        }
        
        log.info("Facture bihebdomadaire générée: {} pour l'organisation {} (période: {} - {})",
                invoiceNumber, organization.getName(), periodStart, periodEnd);
        
        InvoiceDto invoiceDto = toDto(invoice);
        
        // Envoyer un email de notification
        sendInvoiceNotificationEmail(invoiceDto, organization);
        
        return invoiceDto;
    }
    
    /**
     * Crée les lignes de facture à partir des logs d'utilisation.
     */
    private List<InvoiceItem> createInvoiceItems(Invoice invoice, List<UsageLog> usageLogs) {
        List<InvoiceItem> items = new ArrayList<>();
        
        // Agréger par endpoint
        var usageByEndpoint = usageLogs.stream()
                .collect(Collectors.groupingBy(
                        UsageLog::getEndpoint,
                        Collectors.toList()
                ));
        
        for (var entry : usageByEndpoint.entrySet()) {
            String endpoint = entry.getKey();
            List<UsageLog> logs = entry.getValue();
            
            // Calculer le total pour cet endpoint
            long requestCount = logs.size();
            BigDecimal totalCost = logs.stream()
                    .filter(log -> log.getCostUsd() != null)
                    .map(UsageLog::getCostUsd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Calculer le prix unitaire moyen
            BigDecimal unitPrice = requestCount > 0
                    ? totalCost.divide(BigDecimal.valueOf(requestCount), 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setDescription(getEndpointDescription(endpoint));
            item.setQuantity((int) requestCount);
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(totalCost);
            item.setItemType("API_REQUEST");
            
            items.add(item);
        }
        
        // Ajouter une ligne récapitulative pour les tokens si nécessaire
        long totalTokens = usageLogs.stream()
                .filter(log -> log.getTokensUsed() != null)
                .mapToLong(UsageLog::getTokensUsed)
                .sum();
        
        if (totalTokens > 0) {
            // Calculer le coût total des tokens
            BigDecimal totalTokenCost = usageLogs.stream()
                    .filter(log -> log.getCostUsd() != null)
                    .map(UsageLog::getCostUsd)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal tokenUnitPrice = totalTokens > 0
                    ? totalTokenCost.divide(BigDecimal.valueOf(totalTokens), 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            InvoiceItem tokenItem = new InvoiceItem();
            tokenItem.setInvoice(invoice);
            tokenItem.setDescription("Utilisation de tokens (agrégé)");
            tokenItem.setQuantity((int) totalTokens);
            tokenItem.setUnitPrice(tokenUnitPrice);
            tokenItem.setTotalPrice(totalTokenCost);
            tokenItem.setItemType("TOKEN_USAGE");
            
            items.add(tokenItem);
        }
        
        return items;
    }
    
    /**
     * Génère un numéro de facture unique.
     * Format: INV-{YYYYMM}-{ORG_ID}-{SEQUENCE}
     */
    private String generateInvoiceNumber(Long organizationId, int year, int month) {
        String baseNumber = String.format("INV-%s-%03d", 
                YearMonth.of(year, month).format(INVOICE_NUMBER_FORMAT),
                organizationId);
        
        // Vérifier l'unicité et ajouter un suffixe si nécessaire
        String invoiceNumber = baseNumber;
        int suffix = 1;
        while (invoiceRepository.findByInvoiceNumber(invoiceNumber).isPresent()) {
            invoiceNumber = baseNumber + "-" + suffix;
            suffix++;
        }
        
        return invoiceNumber;
    }
    
    /**
     * Retourne une description lisible pour un endpoint.
     */
    private String getEndpointDescription(String endpoint) {
        return switch (endpoint) {
            case "/recherche/sections" -> "Recherche par sections";
            case "/recherche/chapitres" -> "Recherche par chapitres";
            case "/recherche/positions4" -> "Recherche par positions (4 chiffres)";
            case "/recherche/positions6" -> "Recherche par positions (6 chiffres)";
            default -> "Requête API: " + endpoint;
        };
    }
    
    /**
     * Récupère une facture par son ID.
     */
    public InvoiceDto getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Facture non trouvée avec l'ID: " + invoiceId));
        return toDto(invoice);
    }
    
    /**
     * Récupère toutes les factures d'une organisation.
     */
    public List<InvoiceDto> getInvoicesByOrganization(Long organizationId) {
        return invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère toutes les factures (admin uniquement).
     */
    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Met à jour le statut d'une facture.
     * Envoie un email de notification si le statut est mis à PENDING.
     */
    @Transactional
    public InvoiceDto updateInvoiceStatus(Long invoiceId, Invoice.InvoiceStatus status, String notes) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Facture non trouvée avec l'ID: " + invoiceId));
        
        Invoice.InvoiceStatus previousStatus = invoice.getStatus();
        invoice.setStatus(status);
        if (notes != null && !notes.trim().isEmpty()) {
            invoice.setNotes(notes);
        }
        
        if (status == Invoice.InvoiceStatus.PAID && invoice.getPaidAt() == null) {
            invoice.setPaidAt(LocalDateTime.now());
        }
        
        invoice = invoiceRepository.save(invoice);
        
        log.info("Statut de la facture {} mis à jour: {} -> {}", invoice.getInvoiceNumber(), previousStatus, status);
        
        InvoiceDto invoiceDto = toDto(invoice);
        
        // Envoyer un email de notification si le statut est mis à PENDING
        // (seulement si le statut précédent n'était pas déjà PENDING pour éviter les doublons)
        if (status == Invoice.InvoiceStatus.PENDING && previousStatus != Invoice.InvoiceStatus.PENDING) {
            OrganizationDto organization = organizationService.getOrganizationById(invoice.getOrganizationId());
            if (organization != null) {
                sendInvoiceNotificationEmail(invoiceDto, organization);
            }
        }
        
        return invoiceDto;
    }
    
    /**
     * Compte les nouvelles factures non consultées d'une organisation.
     */
    public long countNewInvoices(Long organizationId) {
        return invoiceRepository.countByOrganizationIdAndViewedAtIsNull(organizationId);
    }
    
    /**
     * Compte les factures en retard (OVERDUE) d'une organisation.
     */
    public long countOverdueInvoices(Long organizationId) {
        return invoiceRepository.countByOrganizationIdAndStatus(organizationId, Invoice.InvoiceStatus.OVERDUE);
    }
    
    /**
     * Marque une facture comme consultée.
     */
    @Transactional
    public InvoiceDto markInvoiceAsViewed(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Facture non trouvée avec l'ID: " + invoiceId));
        
        // Marquer comme consultée seulement si ce n'est pas déjà fait
        if (invoice.getViewedAt() == null) {
            invoice.setViewedAt(LocalDateTime.now());
            invoice = invoiceRepository.save(invoice);
            log.info("Facture {} marquée comme consultée", invoice.getInvoiceNumber());
        }
        
        return toDto(invoice);
    }
    
    /**
     * Convertit une Invoice en DTO.
     */
    private InvoiceDto toDto(Invoice invoice) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoice.getId());
        dto.setOrganizationId(invoice.getOrganizationId());
        dto.setOrganizationName(invoice.getOrganizationName());
        dto.setOrganizationEmail(invoice.getOrganizationEmail());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setPeriodStart(invoice.getPeriodStart());
        dto.setPeriodEnd(invoice.getPeriodEnd());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setStatus(invoice.getStatus());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setDueDate(invoice.getDueDate());
        dto.setPaidAt(invoice.getPaidAt());
        dto.setNotes(invoice.getNotes());
        dto.setViewedAt(invoice.getViewedAt());
        
        // Récupérer les lignes de facture
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderById(invoice.getId());
        if (items != null) {
            dto.setItems(items.stream()
                    .map(this::toItemDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setItems(new ArrayList<>());
        }
        
        // Calculer les statistiques d'utilisation pour la période
        LocalDateTime startDateTime = invoice.getPeriodStart().atStartOfDay();
        LocalDateTime endDateTime = invoice.getPeriodEnd().atTime(LocalTime.MAX);
        
        Long orgId = invoice.getOrganizationId();
        List<UsageLog> usageLogs = orgId != null 
                ? usageLogRepository.findByOrganizationIdAndTimestampBetween(orgId, startDateTime, endDateTime)
                : new ArrayList<>();
        
        dto.setTotalRequests((long) usageLogs.size());
        dto.setTotalTokens(usageLogs.stream()
                .filter(log -> log.getTokensUsed() != null)
                .mapToLong(UsageLog::getTokensUsed)
                .sum());
        dto.setTotalCostUsd(usageLogs.stream()
                .filter(log -> log.getCostUsd() != null)
                .map(UsageLog::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP));
        
        return dto;
    }
    
    /**
     * Convertit une InvoiceItem en DTO.
     */
    private InvoiceItemDto toItemDto(InvoiceItem item) {
        InvoiceItemDto dto = new InvoiceItemDto();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalPrice(item.getTotalPrice());
        dto.setItemType(item.getItemType());
        return dto;
    }

    /**
     * Envoie un email de notification pour une nouvelle facture.
     * Envoie l'email à l'organisation et à tous les utilisateurs de l'organisation.
     * L'email est envoyé uniquement si le statut de la facture est PENDING (en attente).
     */
    private void sendInvoiceNotificationEmail(InvoiceDto invoice, OrganizationDto organization) {
        // Envoyer l'email uniquement si le statut est PENDING
        if (invoice.getStatus() != Invoice.InvoiceStatus.PENDING) {
            log.debug("Email de notification non envoyé pour la facture {} car le statut est {} (attendu: PENDING)",
                    invoice.getInvoiceNumber(), invoice.getStatus());
            return;
        }
        
        try {
            // Collecter les emails des destinataires
            List<String> recipientEmails = new ArrayList<>();

            // Ajouter l'email de l'organisation s'il existe
            if (organization.getEmail() != null && !organization.getEmail().trim().isEmpty()) {
                recipientEmails.add(organization.getEmail().trim());
            }

            // Récupérer les utilisateurs de l'organisation et leurs emails depuis Keycloak
            List<OrganizationUserDto> organizationUsers = organizationService.getUsersByOrganization(organization.getId());
            List<String> keycloakUserIds = organizationUsers.stream()
                    .map(OrganizationUserDto::getKeycloakUserId)
                    .collect(Collectors.toList());

            // Récupérer les emails des utilisateurs depuis Keycloak
            List<String> userEmails = keycloakAdminService.getUserEmails(keycloakUserIds);
            recipientEmails.addAll(userEmails);

            // Supprimer les doublons
            recipientEmails = recipientEmails.stream()
                    .distinct()
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .collect(Collectors.toList());

            if (recipientEmails.isEmpty()) {
                log.warn("Aucun email trouvé pour envoyer la notification de facture {} à l'organisation {}",
                        invoice.getInvoiceNumber(), organization.getName());
                return;
            }

            // Formater les dates et le montant
            String periodStartFormatted = invoice.getPeriodStart().format(DATE_FORMATTER);
            String periodEndFormatted = invoice.getPeriodEnd().format(DATE_FORMATTER);
            String totalAmountFormatted = String.format("%.2f", invoice.getTotalAmount());

            // Envoyer l'email à tous les destinataires
            emailService.sendInvoiceNotificationEmailToMultiple(
                    recipientEmails,
                    organization.getName(),
                    invoice.getInvoiceNumber(),
                    periodStartFormatted,
                    periodEndFormatted,
                    totalAmountFormatted,
                    invoice.getId()
            );

            log.info("Email de notification de facture {} envoyé à {} destinataire(s) pour l'organisation {}",
                    invoice.getInvoiceNumber(), recipientEmails.size(), organization.getName());
        } catch (Exception e) {
            // Ne pas faire échouer la génération de facture si l'envoi d'email échoue
            log.error("Erreur lors de l'envoi de l'email de notification pour la facture {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Génère les factures mensuelles pour toutes les organisations ayant une utilisation.
     * 
     * @param year Année
     * @param month Mois (1-12)
     * @return Liste des factures générées
     */
    @Transactional
    public List<InvoiceDto> generateMonthlyInvoicesForAllOrganizations(int year, int month) {
        List<InvoiceDto> invoices = new ArrayList<>();
        
        // Récupérer toutes les organisations
        List<OrganizationDto> organizations = organizationService.getAllOrganizations();
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = yearMonth.atEndOfMonth();
        
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(LocalTime.MAX);
        
        for (OrganizationDto organization : organizations) {
            try {
                // Vérifier si une facture existe déjà
                if (invoiceRepository.existsByOrganizationIdAndPeriodStartAndPeriodEnd(
                        organization.getId(), periodStart, periodEnd)) {
                    log.debug("Facture déjà existante pour l'organisation {} pour la période {}",
                            organization.getName(), yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    continue;
                }
                
                // Vérifier s'il y a de l'utilisation pour cette période
                List<UsageLog> usageLogs = usageLogRepository.findByOrganizationIdAndTimestampBetween(
                        organization.getId(), startDateTime, endDateTime);
                
                if (usageLogs.isEmpty()) {
                    log.debug("Aucune utilisation pour l'organisation {} pour la période {}",
                            organization.getName(), yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                    continue;
                }
                
                // Générer la facture
                InvoiceDto invoice = generateMonthlyInvoice(organization.getId(), year, month);
                invoices.add(invoice);
                
                log.info("Facture générée pour l'organisation {}: {}",
                        organization.getName(), invoice.getInvoiceNumber());
                
            } catch (Exception e) {
                log.error("Erreur lors de la génération de la facture pour l'organisation {}: {}",
                        organization.getName(), e.getMessage(), e);
            }
        }
        
        log.info("Génération de factures terminée. {} facture(s) générée(s) pour la période {}",
                invoices.size(), yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        
        return invoices;
    }
    
    /**
     * Tâche planifiée pour marquer automatiquement les factures en retard (OVERDUE).
     * Exécutée quotidiennement à 9h00 du matin.
     */
    @Scheduled(cron = "0 0 9 * * ?") // Tous les jours à 9h00
    @Transactional
    public void markOverdueInvoices() {
        log.info("Démarrage de la vérification des factures en retard...");
        
        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateBefore(
                Invoice.InvoiceStatus.PENDING, today);
        
        if (overdueInvoices.isEmpty()) {
            log.info("Aucune facture en retard trouvée.");
            return;
        }
        
        log.info("{} facture(s) en retard trouvée(s).", overdueInvoices.size());
        
        for (Invoice invoice : overdueInvoices) {
            try {
                // Marquer comme en retard
                invoice.setStatus(Invoice.InvoiceStatus.OVERDUE);
                invoice = invoiceRepository.save(invoice);
                
                log.info("Facture {} marquée comme en retard (date d'échéance: {})",
                        invoice.getInvoiceNumber(), invoice.getDueDate());
                
                // Envoyer un email de rappel
                InvoiceDto invoiceDto = toDto(invoice);
                OrganizationDto organization = organizationService.getOrganizationById(invoice.getOrganizationId());
                if (organization != null) {
                    sendOverdueInvoiceReminderEmail(invoiceDto, organization);
                }
                
            } catch (Exception e) {
                log.error("Erreur lors du traitement de la facture en retard {}: {}",
                        invoice.getInvoiceNumber(), e.getMessage(), e);
            }
        }
        
        log.info("Vérification des factures en retard terminée. {} facture(s) traitée(s).", overdueInvoices.size());
    }
    
    /**
     * Tâche planifiée pour générer automatiquement les factures bihebdomadaires
     * pour les organisations avec un plan Pay-per-Request.
     * Exécutée tous les lundis à 8h00 (toutes les deux semaines).
     */
    @Scheduled(cron = "0 0 8 * * MON") // Tous les lundis à 8h00
    @Transactional
    public void generateBiweeklyInvoicesForPayPerRequestPlans() {
        log.info("=== Démarrage de la génération des factures bihebdomadaires pour les plans Pay-per-Request ===");
        
        try {
            // Récupérer toutes les organisations avec un plan Pay-per-Request
            List<OrganizationDto> organizations = organizationService.getOrganizationsWithPayPerRequestPlan();
            
            if (organizations.isEmpty()) {
                log.info("Aucune organisation avec un plan Pay-per-Request trouvée.");
                return;
            }
            
            log.info("{} organisation(s) avec un plan Pay-per-Request trouvée(s).", organizations.size());
            
            // Calculer la période : 14 derniers jours (du lundi il y a 2 semaines au dimanche dernier)
            LocalDate today = LocalDate.now();
            LocalDate periodEnd = today.minusDays(1); // Hier (dimanche)
            LocalDate periodStart = periodEnd.minusDays(13); // 14 jours avant (lundi il y a 2 semaines)
            
            int invoicesGenerated = 0;
            int invoicesSkipped = 0;
            int errors = 0;
            
            for (OrganizationDto organization : organizations) {
                try {
                    InvoiceDto invoice = generateBiweeklyInvoice(
                            organization.getId(),
                            periodStart,
                            periodEnd);
                    
                    if (invoice != null) {
                        invoicesGenerated++;
                        log.info("✓ Facture bihebdomadaire générée pour l'organisation {} (ID: {})",
                                organization.getName(), organization.getId());
                    } else {
                        invoicesSkipped++;
                        log.debug("Facture non générée pour l'organisation {} (ID: {}) - déjà existante ou aucune utilisation",
                                organization.getName(), organization.getId());
                    }
                } catch (Exception e) {
                    errors++;
                    log.error("✗ Erreur lors de la génération de la facture bihebdomadaire pour l'organisation {} (ID: {}): {}",
                            organization.getName(), organization.getId(), e.getMessage(), e);
                }
            }
            
            log.info("=== Génération des factures bihebdomadaires terminée ===");
            log.info("Résumé: {} facture(s) générée(s), {} facture(s) ignorée(s), {} erreur(s)",
                    invoicesGenerated, invoicesSkipped, errors);
            
        } catch (Exception e) {
            log.error("Erreur critique lors de la génération des factures bihebdomadaires: {}", 
                    e.getMessage(), e);
        }
    }
    
    /**
     * Envoie un email de rappel pour une facture en retard.
     */
    private void sendOverdueInvoiceReminderEmail(InvoiceDto invoice, OrganizationDto organization) {
        try {
            // Collecter les emails des destinataires
            List<String> recipientEmails = new ArrayList<>();

            // Ajouter l'email de l'organisation s'il existe
            if (organization.getEmail() != null && !organization.getEmail().trim().isEmpty()) {
                recipientEmails.add(organization.getEmail().trim());
            }

            // Récupérer les utilisateurs de l'organisation et leurs emails depuis Keycloak
            List<OrganizationUserDto> organizationUsers = organizationService.getUsersByOrganization(organization.getId());
            List<String> keycloakUserIds = organizationUsers.stream()
                    .map(OrganizationUserDto::getKeycloakUserId)
                    .collect(Collectors.toList());

            // Récupérer les emails des utilisateurs depuis Keycloak
            List<String> userEmails = keycloakAdminService.getUserEmails(keycloakUserIds);
            recipientEmails.addAll(userEmails);

            // Supprimer les doublons
            recipientEmails = recipientEmails.stream()
                    .distinct()
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .collect(Collectors.toList());

            if (recipientEmails.isEmpty()) {
                log.warn("Aucun email trouvé pour envoyer le rappel de facture en retard {} à l'organisation {}",
                        invoice.getInvoiceNumber(), organization.getName());
                return;
            }

            // Formater les dates et le montant
            String periodStartFormatted = invoice.getPeriodStart().format(DATE_FORMATTER);
            String periodEndFormatted = invoice.getPeriodEnd().format(DATE_FORMATTER);
            String dueDateFormatted = invoice.getDueDate().format(DATE_FORMATTER);
            String totalAmountFormatted = String.format("%.2f", invoice.getTotalAmount());
            
            // Calculer le nombre de jours de retard
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());

            // Envoyer l'email à tous les destinataires
            emailService.sendOverdueInvoiceReminderEmailToMultiple(
                    recipientEmails,
                    organization.getName(),
                    invoice.getInvoiceNumber(),
                    periodStartFormatted,
                    periodEndFormatted,
                    dueDateFormatted,
                    totalAmountFormatted,
                    daysOverdue,
                    invoice.getId()
            );

            log.info("Email de rappel de facture en retard {} envoyé à {} destinataire(s) pour l'organisation {}",
                    invoice.getInvoiceNumber(), recipientEmails.size(), organization.getName());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de rappel pour la facture en retard {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage(), e);
        }
    }
    
    /**
     * Génère une facture de clôture pour un plan tarifaire lors d'un changement de plan.
     * Cette facture crédite l'organisation pour la période non utilisée du plan actuel (prorata).
     * 
     * @param organizationId ID de l'organisation
     * @param plan Le plan tarifaire qui est clôturé
     * @param changeDate Date du changement de plan
     * @return La facture de clôture générée (avec montant négatif = crédit)
     */
    @Transactional
    public InvoiceDto generatePlanClosureInvoice(Long organizationId, PricingPlanDto plan, LocalDate changeDate) {
        // Vérifier que l'organisation existe
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId);
        }
        
        // Calculer la période de facturation (du début du mois jusqu'à la date de changement)
        LocalDate periodStart = changeDate.withDayOfMonth(1); // Début du mois
        LocalDate periodEnd = changeDate.minusDays(1); // Jour avant le changement
        
        // Calculer le nombre de jours dans le mois
        int daysInMonth = periodStart.lengthOfMonth();
        // Calculer le nombre de jours utilisés (du début du mois jusqu'à la date de changement)
        long daysUsed = ChronoUnit.DAYS.between(periodStart, changeDate);
        // Calculer le nombre de jours non utilisés (crédit)
        long daysUnused = daysInMonth - daysUsed;
        
        // Calculer le montant du crédit (prorata)
        BigDecimal monthlyPrice = plan.getPricePerMonth();
        BigDecimal dailyPrice = monthlyPrice.divide(BigDecimal.valueOf(daysInMonth), 6, RoundingMode.HALF_UP);
        BigDecimal creditAmount = dailyPrice.multiply(BigDecimal.valueOf(daysUnused))
                .setScale(2, RoundingMode.HALF_UP)
                .negate(); // Montant négatif = crédit
        
        // Générer le numéro de facture
        YearMonth yearMonth = YearMonth.from(periodStart);
        String invoiceNumber = generateInvoiceNumber(organizationId, yearMonth.getYear(), yearMonth.getMonthValue()) + "-CLOSURE";
        
        // Vérifier l'unicité du numéro
        int suffix = 1;
        String finalInvoiceNumber = invoiceNumber;
        while (invoiceRepository.findByInvoiceNumber(finalInvoiceNumber).isPresent()) {
            finalInvoiceNumber = invoiceNumber + "-" + suffix;
            suffix++;
        }
        
        // Créer la facture de clôture
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrganizationName(organization.getName());
        invoice.setOrganizationEmail(organization.getEmail());
        invoice.setInvoiceNumber(finalInvoiceNumber);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setTotalAmount(creditAmount); // Montant négatif = crédit
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(changeDate.plusDays(30));
        invoice.setNotes(String.format("Facture de clôture - Crédit prorata pour le plan %s (%d jours non utilisés sur %d jours)", 
            plan.getName(), daysUnused, daysInMonth));
        
        invoice = invoiceRepository.save(invoice);
        
        // Créer une ligne de facture pour le crédit
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(String.format("Crédit prorata - Plan %s (%d jours non utilisés)", plan.getName(), daysUnused));
        item.setQuantity(1);
        item.setUnitPrice(creditAmount);
        item.setTotalPrice(creditAmount);
        item.setItemType("PLAN_CLOSURE_CREDIT");
        
        invoiceItemRepository.save(item);
        
        log.info("Facture de clôture générée: {} pour l'organisation {} (plan: {}, crédit: {} EUR)",
                finalInvoiceNumber, organization.getName(), plan.getName(), creditAmount);
        
        InvoiceDto invoiceDto = toDto(invoice);
        
        // Envoyer un email de notification
        sendInvoiceNotificationEmail(invoiceDto, organization);
        
        return invoiceDto;
    }
    
    /**
     * Génère une facture de démarrage pour un nouveau plan tarifaire lors d'un changement de plan.
     * Cette facture facture l'organisation pour la période restante du mois (prorata).
     * 
     * @param organizationId ID de l'organisation
     * @param plan Le nouveau plan tarifaire
     * @param changeDate Date du changement de plan
     * @return La facture de démarrage générée
     */
    @Transactional
    public InvoiceDto generatePlanStartInvoice(Long organizationId, PricingPlanDto plan, LocalDate changeDate) {
        // Vérifier que l'organisation existe
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId);
        }
        
        // Calculer la période de facturation (de la date de changement jusqu'à la fin du mois)
        LocalDate periodStart = changeDate;
        LocalDate periodEnd = changeDate.withDayOfMonth(changeDate.lengthOfMonth()); // Fin du mois
        
        // Calculer le nombre de jours dans le mois
        int daysInMonth = periodStart.lengthOfMonth();
        // Calculer le nombre de jours restants (de la date de changement jusqu'à la fin du mois)
        long daysRemaining = ChronoUnit.DAYS.between(periodStart, periodEnd.plusDays(1));
        
        // Calculer le montant prorata
        BigDecimal monthlyPrice = plan.getPricePerMonth();
        BigDecimal dailyPrice = monthlyPrice.divide(BigDecimal.valueOf(daysInMonth), 6, RoundingMode.HALF_UP);
        BigDecimal proratedAmount = dailyPrice.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);
        
        // Générer le numéro de facture
        YearMonth yearMonth = YearMonth.from(periodStart);
        String invoiceNumber = generateInvoiceNumber(organizationId, yearMonth.getYear(), yearMonth.getMonthValue()) + "-START";
        
        // Vérifier l'unicité du numéro
        int suffix = 1;
        String finalInvoiceNumber = invoiceNumber;
        while (invoiceRepository.findByInvoiceNumber(finalInvoiceNumber).isPresent()) {
            finalInvoiceNumber = invoiceNumber + "-" + suffix;
            suffix++;
        }
        
        // Créer la facture de démarrage
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrganizationName(organization.getName());
        invoice.setOrganizationEmail(organization.getEmail());
        invoice.setInvoiceNumber(finalInvoiceNumber);
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setTotalAmount(proratedAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(periodEnd.plusDays(30));
        invoice.setNotes(String.format("Facture de démarrage - Plan %s (prorata pour %d jours sur %d jours)", 
            plan.getName(), daysRemaining, daysInMonth));
        
        invoice = invoiceRepository.save(invoice);
        
        // Créer une ligne de facture pour le plan
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(String.format("Plan %s - Prorata (%d jours sur %d jours)", plan.getName(), daysRemaining, daysInMonth));
        item.setQuantity(1);
        item.setUnitPrice(proratedAmount);
        item.setTotalPrice(proratedAmount);
        item.setItemType("PLAN_START");
        
        invoiceItemRepository.save(item);
        
        log.info("Facture de démarrage générée: {} pour l'organisation {} (plan: {}, montant: {} EUR)",
                finalInvoiceNumber, organization.getName(), plan.getName(), proratedAmount);
        
        InvoiceDto invoiceDto = toDto(invoice);
        
        // Envoyer un email de notification
        sendInvoiceNotificationEmail(invoiceDto, organization);
        
        return invoiceDto;
    }
    
    /**
     * Génère une facture de clôture pour un cycle mensuel complet.
     * 
     * @param organizationId ID de l'organisation
     * @param plan Le plan mensuel
     * @param startDate Date de début du cycle
     * @param endDate Date de fin du cycle (inclus)
     * @return La facture générée
     */
    @Transactional
    public InvoiceDto generateMonthlyPlanCycleClosureInvoice(Long organizationId, PricingPlanDto plan, 
                                                             LocalDate startDate, LocalDate endDate) {
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId);
        }
        
        // Vérifier si une facture existe déjà pour cette période
        if (invoiceRepository.existsByOrganizationIdAndPeriodStartAndPeriodEnd(organizationId, startDate, endDate)) {
            log.info("Une facture existe déjà pour l'organisation {} pour la période {} - {}", 
                    organizationId, startDate, endDate);
            return null;
        }
        
        // Le montant est le prix mensuel fixe du plan
        BigDecimal totalAmount = plan.getPricePerMonth();
        
        // Générer le numéro de facture
        YearMonth yearMonth = YearMonth.from(startDate);
        String invoiceNumber = generateInvoiceNumber(organizationId, yearMonth.getYear(), yearMonth.getMonthValue()) + "-CYCLE";
        
        // Vérifier l'unicité du numéro
        int suffix = 1;
        String finalInvoiceNumber = invoiceNumber;
        while (invoiceRepository.findByInvoiceNumber(finalInvoiceNumber).isPresent()) {
            finalInvoiceNumber = invoiceNumber + "-" + suffix;
            suffix++;
        }
        
        // Créer la facture
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrganizationName(organization.getName());
        invoice.setOrganizationEmail(organization.getEmail());
        invoice.setInvoiceNumber(finalInvoiceNumber);
        invoice.setPeriodStart(startDate);
        invoice.setPeriodEnd(endDate);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(endDate.plusDays(30));
        invoice.setNotes(String.format("Facture de cycle mensuel - Plan %s (du %s au %s inclus)", 
                plan.getName(), startDate, endDate));
        
        invoice = invoiceRepository.save(invoice);
        
        // Créer une ligne de facture
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setDescription(String.format("Plan %s - Cycle mensuel (du %s au %s inclus)", 
                plan.getName(), startDate, endDate));
        item.setQuantity(1);
        item.setUnitPrice(totalAmount);
        item.setTotalPrice(totalAmount);
        item.setItemType("MONTHLY_PLAN_CYCLE");
        
        invoiceItemRepository.save(item);
        
        log.info("Facture de cycle mensuel générée: {} pour l'organisation {} (plan: {}, montant: {} {})",
                finalInvoiceNumber, organization.getName(), plan.getName(), totalAmount, plan.getCurrency());
        
        InvoiceDto invoiceDto = toDto(invoice);
        sendInvoiceNotificationEmail(invoiceDto, organization);
        
        return invoiceDto;
    }
    
    /**
     * Génère une facture pour un cycle mensuel (reconduction automatique).
     * 
     * @param organizationId ID de l'organisation
     * @param plan Le plan mensuel
     * @param startDate Date de début du cycle
     * @param endDate Date de fin du cycle (inclus)
     * @return La facture générée
     */
    @Transactional
    public InvoiceDto generateMonthlyPlanCycleInvoice(Long organizationId, PricingPlanDto plan, 
                                                      LocalDate startDate, LocalDate endDate) {
        // Même logique que generateMonthlyPlanCycleClosureInvoice
        return generateMonthlyPlanCycleClosureInvoice(organizationId, plan, startDate, endDate);
    }
    
    /**
     * Génère une facture de clôture pour un plan Pay-per-Request (depuis la dernière facture jusqu'à aujourd'hui).
     * 
     * @param organizationId ID de l'organisation
     * @param plan Le plan Pay-per-Request
     * @param startDate Date de début (dernière facture)
     * @param endDate Date de fin (aujourd'hui)
     * @return La facture générée
     */
    @Transactional
    public InvoiceDto generatePayPerRequestClosureInvoice(Long organizationId, PricingPlanDto plan, 
                                                         LocalDate startDate, LocalDate endDate) {
        OrganizationDto organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId);
        }
        
        // Vérifier si une facture existe déjà pour cette période
        if (invoiceRepository.existsByOrganizationIdAndPeriodStartAndPeriodEnd(organizationId, startDate, endDate)) {
            log.info("Une facture existe déjà pour l'organisation {} pour la période {} - {}", 
                    organizationId, startDate, endDate);
            return null;
        }
        
        // Récupérer les logs d'utilisation pour la période
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<UsageLog> usageLogs = usageLogRepository.findByOrganizationIdAndTimestampBetween(
                organizationId, startDateTime, endDateTime);
        
        // Si aucune utilisation, ne pas générer de facture
        if (usageLogs.isEmpty()) {
            log.info("Aucune utilisation trouvée pour l'organisation {} sur la période {} - {}. Facture non générée.",
                    organizationId, startDate, endDate);
            return null;
        }
        
        // Calculer le total (somme des coûts des requêtes)
        BigDecimal totalAmount = usageLogs.stream()
                .filter(log -> log.getCostUsd() != null)
                .map(UsageLog::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        
        // Générer le numéro de facture
        YearMonth yearMonth = YearMonth.from(startDate);
        String invoiceNumber = generateInvoiceNumber(organizationId, yearMonth.getYear(), yearMonth.getMonthValue()) + "-PPR-CLOSURE";
        
        // Vérifier l'unicité du numéro
        int suffix = 1;
        String finalInvoiceNumber = invoiceNumber;
        while (invoiceRepository.findByInvoiceNumber(finalInvoiceNumber).isPresent()) {
            finalInvoiceNumber = invoiceNumber + "-" + suffix;
            suffix++;
        }
        
        // Créer la facture
        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setOrganizationName(organization.getName());
        invoice.setOrganizationEmail(organization.getEmail());
        invoice.setInvoiceNumber(finalInvoiceNumber);
        invoice.setPeriodStart(startDate);
        invoice.setPeriodEnd(endDate);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setDueDate(endDate.plusDays(14));
        invoice.setNotes(String.format("Facture de clôture Pay-per-Request - Plan %s (du %s au %s)", 
                plan.getName(), startDate, endDate));
        
        invoice = invoiceRepository.save(invoice);
        
        // Créer les lignes de facture
        List<InvoiceItem> items = createInvoiceItems(invoice, usageLogs);
        if (!items.isEmpty()) {
            invoiceItemRepository.saveAll(items);
        }
        
        log.info("Facture de clôture Pay-per-Request générée: {} pour l'organisation {} (plan: {}, montant: {} {})",
                finalInvoiceNumber, organization.getName(), plan.getName(), totalAmount, plan.getCurrency());
        
        InvoiceDto invoiceDto = toDto(invoice);
        sendInvoiceNotificationEmail(invoiceDto, organization);
        
        return invoiceDto;
    }
}

