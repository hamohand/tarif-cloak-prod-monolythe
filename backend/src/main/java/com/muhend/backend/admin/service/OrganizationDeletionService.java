package com.muhend.backend.admin.service;

import com.muhend.backend.alert.repository.QuotaAlertRepository;
import com.muhend.backend.invoice.repository.InvoiceItemRepository;
import com.muhend.backend.invoice.repository.InvoiceRepository;
import com.muhend.backend.organization.model.Organization;
import com.muhend.backend.organization.repository.OrganizationRepository;
import com.muhend.backend.organization.repository.OrganizationUserRepository;
import com.muhend.backend.payment.repository.PaymentRepository;
import com.muhend.backend.payment.repository.SubscriptionRepository;
import com.muhend.backend.pricing.repository.QuoteRequestRepository;
import com.muhend.backend.usage.repository.UsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service pour supprimer définitivement une organisation et tous ses éléments associés
 */
@Service
public class OrganizationDeletionService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrganizationDeletionService.class);
    
    private final OrganizationRepository organizationRepository;
    private final UsageLogRepository usageLogRepository;
    private final QuotaAlertRepository quotaAlertRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final QuoteRequestRepository quoteRequestRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    
    public OrganizationDeletionService(
            OrganizationRepository organizationRepository,
            UsageLogRepository usageLogRepository,
            QuotaAlertRepository quotaAlertRepository,
            OrganizationUserRepository organizationUserRepository,
            QuoteRequestRepository quoteRequestRepository,
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            PaymentRepository paymentRepository,
            SubscriptionRepository subscriptionRepository) {
        this.organizationRepository = organizationRepository;
        this.usageLogRepository = usageLogRepository;
        this.quotaAlertRepository = quotaAlertRepository;
        this.organizationUserRepository = organizationUserRepository;
        this.quoteRequestRepository = quoteRequestRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }
    
    /**
     * Supprime définitivement une organisation et tous ses éléments associés
     * 
     * Ordre de suppression (pour respecter les contraintes de clés étrangères) :
     * 1. InvoiceItems (liés aux invoices)
     * 2. Invoices (liés à l'organisation)
     * 3. Payments (liés à l'organisation)
     * 4. Subscriptions (liés à l'organisation)
     * 5. QuoteRequests (liés à l'organisation)
     * 6. UsageLogs (liés à l'organisation)
     * 7. QuotaAlerts (liés à l'organisation)
     * 8. OrganizationUsers (liés à l'organisation)
     * 9. Organization (l'organisation elle-même)
     * 
     * @param organizationId ID de l'organisation à supprimer
     * @return Résultat de la suppression avec statistiques
     */
    @Transactional
    public DeletionResult deleteOrganization(Long organizationId) {
        logger.warn("=== DÉBUT DE LA SUPPRESSION DE L'ORGANISATION {} ===", organizationId);
        
        // Vérifier que l'organisation existe
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organisation non trouvée avec l'ID: " + organizationId));
        
        String organizationName = organization.getName();
        logger.info("Suppression de l'organisation: {} (ID: {})", organizationName, organizationId);
        
        DeletionResult result = new DeletionResult();
        result.setOrganizationName(organizationName);
        result.setOrganizationId(organizationId);
        
        try {
            // 1. Supprimer les InvoiceItems (liés aux invoices de l'organisation)
            // Récupérer les IDs des factures avant de les supprimer
            List<Long> invoiceIds = invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(invoice -> invoice.getId())
                .toList();
            
            for (Long invoiceId : invoiceIds) {
                int deletedItems = invoiceItemRepository.deleteByInvoiceId(invoiceId);
                result.addDeletedInvoiceItems(deletedItems);
                if (deletedItems > 0) {
                    logger.debug("  - {} éléments de facture supprimés pour la facture {}", deletedItems, invoiceId);
                }
            }
            
            // 2. Supprimer les Invoices
            int deletedInvoices = invoiceRepository.deleteByOrganizationId(organizationId);
            result.setDeletedInvoices(deletedInvoices);
            logger.info("  - {} factures supprimées", deletedInvoices);
            
            // 3. Supprimer les Payments
            int deletedPayments = paymentRepository.deleteByOrganizationId(organizationId);
            result.setDeletedPayments(deletedPayments);
            logger.info("  - {} paiements supprimés", deletedPayments);
            
            // 4. Supprimer les Subscriptions
            int deletedSubscriptions = subscriptionRepository.deleteByOrganizationId(organizationId);
            result.setDeletedSubscriptions(deletedSubscriptions);
            logger.info("  - {} abonnements supprimés", deletedSubscriptions);
            
            // 5. Supprimer les QuoteRequests
            int deletedQuoteRequests = quoteRequestRepository.deleteByOrganizationId(organizationId);
            result.setDeletedQuoteRequests(deletedQuoteRequests);
            logger.info("  - {} demandes de devis supprimées", deletedQuoteRequests);
            
            // 6. Supprimer les UsageLogs
            int deletedUsageLogs = usageLogRepository.deleteByOrganizationId(organizationId);
            result.setDeletedUsageLogs(deletedUsageLogs);
            logger.info("  - {} logs d'utilisation supprimés", deletedUsageLogs);
            
            // 7. Supprimer les QuotaAlerts
            int deletedQuotaAlerts = quotaAlertRepository.deleteByOrganizationId(organizationId);
            result.setDeletedQuotaAlerts(deletedQuotaAlerts);
            logger.info("  - {} alertes de quota supprimées", deletedQuotaAlerts);
            
            // 8. Supprimer les OrganizationUsers
            int deletedOrganizationUsers = organizationUserRepository.deleteByOrganizationId(organizationId);
            result.setDeletedOrganizationUsers(deletedOrganizationUsers);
            logger.info("  - {} associations utilisateur-organisation supprimées", deletedOrganizationUsers);
            
            // 9. Supprimer l'organisation elle-même
            organizationRepository.delete(organization);
            logger.info("  ✓ Organisation {} supprimée avec succès", organization.getName());
            
            result.setSuccess(true);
            logger.info("=== SUPPRESSION TERMINÉE AVEC SUCCÈS ===");
            
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de l'organisation {}: {}", 
                organizationId, e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw new RuntimeException("Erreur lors de la suppression de l'organisation: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Classe pour stocker le résultat de la suppression
     */
    public static class DeletionResult {
        private Long organizationId;
        private String organizationName;
        private boolean success;
        private String errorMessage;
        private long deletedInvoiceItems = 0;
        private long deletedInvoices = 0;
        private long deletedPayments = 0;
        private long deletedSubscriptions = 0;
        private long deletedQuoteRequests = 0;
        private long deletedUsageLogs = 0;
        private long deletedQuotaAlerts = 0;
        private long deletedOrganizationUsers = 0;
        
        public Long getOrganizationId() {
            return organizationId;
        }
        
        public void setOrganizationId(Long organizationId) {
            this.organizationId = organizationId;
        }
        
        public String getOrganizationName() {
            return organizationName;
        }
        
        public void setOrganizationName(String organizationName) {
            this.organizationName = organizationName;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public long getDeletedInvoiceItems() {
            return deletedInvoiceItems;
        }
        
        public void addDeletedInvoiceItems(int count) {
            this.deletedInvoiceItems += count;
        }
        
        public long getDeletedInvoices() {
            return deletedInvoices;
        }
        
        public void setDeletedInvoices(long deletedInvoices) {
            this.deletedInvoices = deletedInvoices;
        }
        
        public long getDeletedPayments() {
            return deletedPayments;
        }
        
        public void setDeletedPayments(long deletedPayments) {
            this.deletedPayments = deletedPayments;
        }
        
        public long getDeletedSubscriptions() {
            return deletedSubscriptions;
        }
        
        public void setDeletedSubscriptions(long deletedSubscriptions) {
            this.deletedSubscriptions = deletedSubscriptions;
        }
        
        public long getDeletedQuoteRequests() {
            return deletedQuoteRequests;
        }
        
        public void setDeletedQuoteRequests(long deletedQuoteRequests) {
            this.deletedQuoteRequests = deletedQuoteRequests;
        }
        
        public long getDeletedUsageLogs() {
            return deletedUsageLogs;
        }
        
        public void setDeletedUsageLogs(long deletedUsageLogs) {
            this.deletedUsageLogs = deletedUsageLogs;
        }
        
        public long getDeletedQuotaAlerts() {
            return deletedQuotaAlerts;
        }
        
        public void setDeletedQuotaAlerts(long deletedQuotaAlerts) {
            this.deletedQuotaAlerts = deletedQuotaAlerts;
        }
        
        public long getDeletedOrganizationUsers() {
            return deletedOrganizationUsers;
        }
        
        public void setDeletedOrganizationUsers(long deletedOrganizationUsers) {
            this.deletedOrganizationUsers = deletedOrganizationUsers;
        }
    }
}

