package com.muhend.backend.invoice.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.muhend.backend.invoice.dto.InvoiceDto;
import com.muhend.backend.invoice.dto.InvoiceItemDto;
import com.muhend.backend.invoice.model.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Service pour générer des factures en PDF.
 * Phase 5 MVP : Facturation
 */
@Service
@Slf4j
public class InvoicePdfService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    /**
     * Génère un PDF à partir d'une facture.
     * 
     * @param invoice La facture à convertir en PDF
     * @return Le contenu du PDF sous forme de byte array
     */
    public byte[] generatePdf(InvoiceDto invoice) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (PdfWriter writer = new PdfWriter(outputStream);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.A4)) {
            
            // En-tête
            addHeader(document, invoice);
            
            // Informations de la facture et de l'organisation
            addInvoiceInfo(document, invoice);
            
            // Lignes de facture
            addInvoiceItems(document, invoice);
            
            // Total
            addTotal(document, invoice);
            
            // Notes
            if (invoice.getNotes() != null && !invoice.getNotes().trim().isEmpty()) {
                addNotes(document, invoice.getNotes());
            }
            
            // Pied de page
            addFooter(document);
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF pour la facture {}", invoice.getInvoiceNumber(), e);
            throw new IOException("Erreur lors de la génération du PDF", e);
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Ajoute l'en-tête de la facture.
     */
    private void addHeader(Document document, InvoiceDto invoice) {
        Paragraph title = new Paragraph("FACTURE")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);
        
        Paragraph invoiceNumber = new Paragraph("N° " + invoice.getInvoiceNumber())
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(invoiceNumber);
    }
    
    /**
     * Ajoute les informations de la facture et de l'organisation.
     */
    private void addInvoiceInfo(Document document, InvoiceDto invoice) {
        Table infoTable = new Table(2)
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
        
        // Colonne gauche : Organisation
        Cell orgCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("FACTURÉ À :")
                        .setBold()
                        .setMarginBottom(5))
                .add(new Paragraph(invoice.getOrganizationName())
                        .setMarginBottom(2));
        
        if (invoice.getOrganizationEmail() != null && !invoice.getOrganizationEmail().trim().isEmpty()) {
            orgCell.add(new Paragraph(invoice.getOrganizationEmail())
                    .setMarginBottom(2));
        }
        
        infoTable.addCell(orgCell);
        
        // Colonne droite : Informations de facturation
        Cell invoiceInfoCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("PÉRIODE :")
                        .setBold()
                        .setMarginBottom(5))
                .add(new Paragraph(
                        invoice.getPeriodStart().format(DATE_FORMATTER) + " - " +
                                invoice.getPeriodEnd().format(DATE_FORMATTER))
                        .setMarginBottom(10))
                .add(new Paragraph("DATE DE FACTURATION :")
                        .setBold()
                        .setMarginBottom(5))
                .add(new Paragraph(invoice.getCreatedAt().format(DATETIME_FORMATTER))
                        .setMarginBottom(10))
                .add(new Paragraph("DATE D'ÉCHÉANCE :")
                        .setBold()
                        .setMarginBottom(5))
                .add(new Paragraph(invoice.getDueDate().format(DATE_FORMATTER))
                        .setMarginBottom(10))
                .add(new Paragraph("STATUT :")
                        .setBold()
                        .setMarginBottom(5))
                .add(new Paragraph(getStatusText(invoice.getStatus())));
        
        infoTable.addCell(invoiceInfoCell);
        
        document.add(infoTable);
    }
    
    /**
     * Ajoute les lignes de facture.
     */
    private void addInvoiceItems(Document document, InvoiceDto invoice) {
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            return;
        }
        
        Table itemsTable = new Table(4)
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
        
        // En-tête du tableau
        itemsTable.addHeaderCell(new Cell()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .add(new Paragraph("Description")));
        itemsTable.addHeaderCell(new Cell()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph("Quantité")));
        itemsTable.addHeaderCell(new Cell()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Prix unitaire")));
        itemsTable.addHeaderCell(new Cell()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Total")));
        
        // Lignes de facture
        for (InvoiceItemDto item : invoice.getItems()) {
            itemsTable.addCell(new Cell().add(new Paragraph(item.getDescription())));
            itemsTable.addCell(new Cell()
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph(String.valueOf(item.getQuantity()))));
            itemsTable.addCell(new Cell()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(formatCurrency(item.getUnitPrice()))));
            itemsTable.addCell(new Cell()
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph(formatCurrency(item.getTotalPrice()))));
        }
        
        document.add(itemsTable);
    }
    
    /**
     * Ajoute le total de la facture.
     */
    private void addTotal(Document document, InvoiceDto invoice) {
        Table totalTable = new Table(2)
                .setWidth(UnitValue.createPercentValue(50))
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setMarginBottom(20);
        
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .add(new Paragraph("TOTAL HT :")));
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .add(new Paragraph(formatCurrency(invoice.getTotalAmount()))));
        
        // TVA (si applicable, pour l'instant 0%)
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("TVA (0%) :")));
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(formatCurrency(BigDecimal.ZERO))));
        
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .setFontSize(14)
                .add(new Paragraph("TOTAL TTC :")));
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .setFontSize(14)
                .add(new Paragraph(formatCurrency(invoice.getTotalAmount()))));
        
        document.add(totalTable);
    }
    
    /**
     * Ajoute les notes de la facture.
     */
    private void addNotes(Document document, String notes) {
        Paragraph notesParagraph = new Paragraph("Notes :")
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(5);
        document.add(notesParagraph);
        
        Paragraph notesContent = new Paragraph(notes)
                .setMarginBottom(20);
        document.add(notesContent);
    }
    
    /**
     * Ajoute le pied de page.
     */
    private void addFooter(Document document) {
        Paragraph footer = new Paragraph(
                "Merci de votre confiance. Pour toute question, contactez-nous à support@example.com")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setFontColor(ColorConstants.GRAY);
        document.add(footer);
    }
    
    /**
     * Retourne le texte du statut en français.
     */
    private String getStatusText(Invoice.InvoiceStatus status) {
        return switch (status) {
            case DRAFT -> "Brouillon";
            case PENDING -> "En attente de paiement";
            case PAID -> "Payée";
            case OVERDUE -> "En retard";
            case CANCELLED -> "Annulée";
        };
    }
    
    /**
     * Formate un montant en devise.
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0,00 €";
        }
        return String.format("%.2f €", amount.doubleValue()).replace(".", ",");
    }
}

