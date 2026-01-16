package com.muhend.backend.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

/**
 * Service pour l'envoi d'emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.from-name:Enclume Numérique}")
    private String fromName;

    @Value("${FRONTEND_URL:https://hscode.enclume-numerique.com}")
    private String frontendUrl;

    /**
     * Envoie un email de notification pour une nouvelle facture.
     *
     * @param toEmail Email du destinataire
     * @param organizationName Nom de l'organisation
     * @param invoiceNumber Numéro de la facture
     * @param periodStart Date de début de période
     * @param periodEnd Date de fin de période
     * @param totalAmount Montant total de la facture
     * @param invoiceId ID de la facture (pour le lien)
     */
    public void sendInvoiceNotificationEmail(
            String toEmail,
            String organizationName,
            String invoiceNumber,
            String periodStart,
            String periodEnd,
            String totalAmount,
            Long invoiceId) {
        try {
            Context context = new Context();
            context.setVariable("organizationName", organizationName);
            context.setVariable("invoiceNumber", invoiceNumber);
            context.setVariable("periodStart", periodStart);
            context.setVariable("periodEnd", periodEnd);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("invoiceId", invoiceId);
            context.setVariable("frontendUrl", getFrontendUrl());

            String htmlContent = templateEngine.process("invoice-notification", context);
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new RuntimeException("Le template d'email a généré un contenu vide");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = fromEmail != null ? fromEmail : "noreply@enclume-numerique.com";
            String fromNameValue = fromName != null ? fromName : "Enclume Numérique";
            
            helper.setFrom(from, fromNameValue);
            helper.setTo(toEmail != null ? toEmail : "");
            helper.setSubject("Nouvelle facture disponible - " + (invoiceNumber != null ? invoiceNumber : ""));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de notification de facture envoyé à {} pour la facture {}", toEmail, invoiceNumber);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de notification de facture à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'envoi de l'email à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    /**
     * Envoie un email de notification pour une nouvelle facture à plusieurs destinataires.
     */
    public void sendInvoiceNotificationEmailToMultiple(
            List<String> toEmails,
            String organizationName,
            String invoiceNumber,
            String periodStart,
            String periodEnd,
            String totalAmount,
            Long invoiceId) {
        for (String email : toEmails) {
            if (email != null && !email.trim().isEmpty()) {
                try {
                    sendInvoiceNotificationEmail(email, organizationName, invoiceNumber, periodStart, periodEnd, totalAmount, invoiceId);
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi de l'email à {}: {}", email, e.getMessage());
                    // Continuer avec les autres emails même si un échoue
                }
            }
        }
    }

    /**
     * Envoie un email de rappel pour une facture en retard.
     *
     * @param toEmail Email du destinataire
     * @param organizationName Nom de l'organisation
     * @param invoiceNumber Numéro de la facture
     * @param periodStart Date de début de période
     * @param periodEnd Date de fin de période
     * @param dueDate Date d'échéance
     * @param totalAmount Montant total de la facture
     * @param daysOverdue Nombre de jours de retard
     * @param invoiceId ID de la facture (pour le lien)
     */
    public void sendOverdueInvoiceReminderEmail(
            String toEmail,
            String organizationName,
            String invoiceNumber,
            String periodStart,
            String periodEnd,
            String dueDate,
            String totalAmount,
            long daysOverdue,
            Long invoiceId) {
        try {
            Context context = new Context();
            context.setVariable("organizationName", organizationName);
            context.setVariable("invoiceNumber", invoiceNumber);
            context.setVariable("periodStart", periodStart);
            context.setVariable("periodEnd", periodEnd);
            context.setVariable("dueDate", dueDate);
            context.setVariable("totalAmount", totalAmount);
            context.setVariable("daysOverdue", daysOverdue);
            context.setVariable("invoiceId", invoiceId);
            context.setVariable("frontendUrl", getFrontendUrl());

            String htmlContent = templateEngine.process("invoice-overdue-reminder", context);
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new RuntimeException("Le template d'email a généré un contenu vide");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = fromEmail != null ? fromEmail : "noreply@enclume-numerique.com";
            String fromNameValue = fromName != null ? fromName : "Enclume Numérique";
            
            helper.setFrom(from, fromNameValue);
            helper.setTo(toEmail != null ? toEmail : "");
            helper.setSubject("⚠️ Facture en retard - " + (invoiceNumber != null ? invoiceNumber : "") + " (" + daysOverdue + " jour(s) de retard)");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de rappel de facture en retard envoyé à {} pour la facture {} ({} jours de retard)", 
                    toEmail, invoiceNumber, daysOverdue);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de rappel de facture en retard à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'envoi de l'email de rappel à {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    /**
     * Envoie un email de rappel pour une facture en retard à plusieurs destinataires.
     */
    public void sendOverdueInvoiceReminderEmailToMultiple(
            List<String> toEmails,
            String organizationName,
            String invoiceNumber,
            String periodStart,
            String periodEnd,
            String dueDate,
            String totalAmount,
            long daysOverdue,
            Long invoiceId) {
        for (String email : toEmails) {
            if (email != null && !email.trim().isEmpty()) {
                try {
                    sendOverdueInvoiceReminderEmail(email, organizationName, invoiceNumber, periodStart, 
                            periodEnd, dueDate, totalAmount, daysOverdue, invoiceId);
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi de l'email de rappel à {}: {}", email, e.getMessage());
                    // Continuer avec les autres emails même si un échoue
                }
            }
        }
    }

    /**
     * Envoie un email de confirmation d'inscription.
     *
     * @param organizationEmail Email de l'organisation (destinataire)
     * @param userEmail Email de l'utilisateur qui s'inscrit
     * @param organizationName Nom de l'organisation
     * @param confirmationUrl URL de confirmation
     * @param isExistingOrganization true si l'organisation existe déjà, false si c'est une nouvelle organisation
     */
    public void sendRegistrationConfirmationEmail(
            String organizationEmail,
            String userEmail,
            String organizationName,
            String confirmationUrl,
            boolean isExistingOrganization) {
        // Vérifier que la configuration SMTP est valide
        String smtpUsername = System.getenv("SMTP_USERNAME");
        String smtpPassword = System.getenv("SMTP_PASSWORD");
        if (smtpUsername == null || smtpUsername.trim().isEmpty() || 
            smtpPassword == null || smtpPassword.trim().isEmpty()) {
            log.error("Configuration SMTP incomplète : SMTP_USERNAME et/ou SMTP_PASSWORD ne sont pas définis. " +
                     "Veuillez configurer ces variables d'environnement dans votre fichier .env");
            throw new IllegalStateException(
                "Configuration SMTP manquante. Les variables SMTP_USERNAME et SMTP_PASSWORD doivent être définies. " +
                "Consultez ENV_VARIABLES.md pour plus d'informations."
            );
        }
        
        try {
            Context context = new Context();
            context.setVariable("organizationEmail", organizationEmail != null ? organizationEmail : "");
            context.setVariable("userEmail", userEmail != null ? userEmail : "");
            context.setVariable("organizationName", organizationName != null ? organizationName : "");
            context.setVariable("confirmationUrl", confirmationUrl != null ? confirmationUrl : "");
            context.setVariable("isExistingOrganization", isExistingOrganization);
            context.setVariable("frontendUrl", getFrontendUrl());

            String htmlContent = templateEngine.process("registration-confirmation", context);
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new RuntimeException("Le template d'email a généré un contenu vide");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = fromEmail != null ? fromEmail : "noreply@enclume-numerique.com";
            String fromNameValue = fromName != null ? fromName : "Enclume Numérique";
            
            helper.setFrom(from, fromNameValue);
            helper.setTo(organizationEmail != null ? organizationEmail : "");
            
            String subject = isExistingOrganization 
                ? "Nouvelle demande d'inscription - " + (organizationName != null ? organizationName : "")
                : "Confirmation de création d'organisation - " + (organizationName != null ? organizationName : "");
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de confirmation d'inscription envoyé à {} pour l'organisation {}", 
                organizationEmail, organizationName);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation d'inscription à {}: {}", 
                organizationEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'envoi de l'email de confirmation à {}: {}", 
                organizationEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    public void sendCollaboratorInvitationEmail(
            String collaboratorEmail,
            String collaboratorFirstName,
            String collaboratorLastName,
            String collaboratorUsername,
            String organizationName,
            String organizationEmail,
            String confirmationUrl,
            String temporaryPassword) {
        String smtpUsername = System.getenv("SMTP_USERNAME");
        String smtpPassword = System.getenv("SMTP_PASSWORD");
        if (smtpUsername == null || smtpUsername.trim().isEmpty() ||
            smtpPassword == null || smtpPassword.trim().isEmpty()) {
            log.error("Configuration SMTP incomplète : SMTP_USERNAME et/ou SMTP_PASSWORD ne sont pas définis.");
            throw new IllegalStateException(
                    "Configuration SMTP manquante. Les variables SMTP_USERNAME et SMTP_PASSWORD doivent être définies.");
        }
        if (collaboratorEmail == null || collaboratorEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("L'email du collaborateur est obligatoire pour envoyer une invitation.");
        }

        try {
            Context context = new Context();
            context.setVariable("collaboratorFirstName", collaboratorFirstName != null ? collaboratorFirstName : "");
            context.setVariable("collaboratorLastName", collaboratorLastName != null ? collaboratorLastName : "");
            context.setVariable("collaboratorUsername", collaboratorUsername != null ? collaboratorUsername : "");
            context.setVariable("organizationName", organizationName != null ? organizationName : "");
            context.setVariable("organizationEmail", organizationEmail != null ? organizationEmail : "");
            context.setVariable("confirmationUrl", confirmationUrl != null ? confirmationUrl : "");
            context.setVariable("frontendUrl", getFrontendUrl());
            context.setVariable("temporaryPassword", temporaryPassword != null ? temporaryPassword : "");

            String htmlContent = templateEngine.process("collaborator-invitation", context);
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new RuntimeException("Le template d'invitation collaborateur a généré un contenu vide");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = fromEmail != null ? fromEmail : "noreply@enclume-numerique.com";
            String fromNameValue = fromName != null ? fromName : "Enclume Numérique";

            helper.setFrom(from, fromNameValue);
            helper.setTo(collaboratorEmail);

            String subject = "Invitation à rejoindre " + (organizationName != null ? organizationName : "votre organisation");
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Invitation collaborateur envoyée à {} pour l'organisation {}", collaboratorEmail, organizationName);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'invitation collaborateur à {}: {}", collaboratorEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'invitation collaborateur", e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'envoi de l'invitation collaborateur à {}: {}", collaboratorEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'invitation collaborateur", e);
        }
    }

    /**
     * Envoie un email de notification de changement de plan tarifaire.
     *
     * @param toEmail Email du destinataire
     * @param organizationName Nom de l'organisation
     * @param oldPlanName Nom de l'ancien plan (peut être null)
     * @param oldPlanPricePerMonth Prix mensuel de l'ancien plan (peut être null)
     * @param oldPlanPricePerRequest Prix par requête de l'ancien plan (peut être null)
     * @param oldPlanQuota Quota mensuel de l'ancien plan (peut être null)
     * @param newPlanName Nom du nouveau plan (peut être null si le plan est retiré)
     * @param newPlanDescription Description du nouveau plan (peut être null)
     * @param newPlanPricePerMonth Prix mensuel du nouveau plan (peut être null)
     * @param newPlanPricePerRequest Prix par requête du nouveau plan (peut être null)
     * @param newPlanQuota Quota mensuel du nouveau plan (peut être null)
     * @param trialPeriodDays Nombre de jours de période d'essai (peut être null)
     * @param trialExpiresAt Date d'expiration de la période d'essai (peut être null)
     */
    public void sendPricingPlanChangedEmail(
            String toEmail,
            String organizationName,
            String oldPlanName,
            java.math.BigDecimal oldPlanPricePerMonth,
            java.math.BigDecimal oldPlanPricePerRequest,
            Integer oldPlanQuota,
            String newPlanName,
            String newPlanDescription,
            java.math.BigDecimal newPlanPricePerMonth,
            java.math.BigDecimal newPlanPricePerRequest,
            Integer newPlanQuota,
            Integer trialPeriodDays,
            String trialExpiresAt) {
        try {
            Context context = new Context();
            context.setVariable("organizationName", organizationName != null ? organizationName : "");
            context.setVariable("oldPlanName", oldPlanName);
            context.setVariable("oldPlanPricePerMonth", oldPlanPricePerMonth);
            context.setVariable("oldPlanPricePerRequest", oldPlanPricePerRequest);
            context.setVariable("oldPlanQuota", oldPlanQuota);
            context.setVariable("newPlanName", newPlanName);
            context.setVariable("newPlanDescription", newPlanDescription);
            context.setVariable("newPlanPricePerMonth", newPlanPricePerMonth);
            context.setVariable("newPlanPricePerRequest", newPlanPricePerRequest);
            context.setVariable("newPlanQuota", newPlanQuota);
            context.setVariable("trialPeriodDays", trialPeriodDays);
            context.setVariable("trialExpiresAt", trialExpiresAt);
            context.setVariable("frontendUrl", getFrontendUrl());

            String htmlContent = templateEngine.process("pricing-plan-changed", context);
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                throw new RuntimeException("Le template d'email a généré un contenu vide");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = fromEmail != null ? fromEmail : "noreply@enclume-numerique.com";
            String fromNameValue = fromName != null ? fromName : "Enclume Numérique";
            
            helper.setFrom(from, fromNameValue);
            helper.setTo(toEmail != null ? toEmail : "");
            helper.setSubject("Changement de plan tarifaire - " + (organizationName != null ? organizationName : ""));
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de notification de changement de plan envoyé à {} pour l'organisation {}", 
                    toEmail, organizationName);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de notification de changement de plan à {}: {}", 
                    toEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'envoi de l'email de notification de changement de plan à {}: {}", 
                    toEmail, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    /**
     * Envoie un email de notification de changement de plan tarifaire à plusieurs destinataires.
     */
    public void sendPricingPlanChangedEmailToMultiple(
            List<String> toEmails,
            String organizationName,
            String oldPlanName,
            java.math.BigDecimal oldPlanPricePerMonth,
            java.math.BigDecimal oldPlanPricePerRequest,
            Integer oldPlanQuota,
            String newPlanName,
            String newPlanDescription,
            java.math.BigDecimal newPlanPricePerMonth,
            java.math.BigDecimal newPlanPricePerRequest,
            Integer newPlanQuota,
            Integer trialPeriodDays,
            String trialExpiresAt) {
        for (String email : toEmails) {
            if (email != null && !email.trim().isEmpty()) {
                try {
                    sendPricingPlanChangedEmail(email, organizationName, oldPlanName, 
                            oldPlanPricePerMonth, oldPlanPricePerRequest, oldPlanQuota,
                            newPlanName, newPlanDescription, newPlanPricePerMonth, 
                            newPlanPricePerRequest, newPlanQuota, trialPeriodDays, trialExpiresAt);
                } catch (Exception e) {
                    log.error("Erreur lors de l'envoi de l'email de notification de changement de plan à {}: {}", 
                            email, e.getMessage());
                    // Continuer avec les autres emails même si un échoue
                }
            }
        }
    }

    /**
     * Envoie une notification à l'administrateur lors de la création d'un nouveau compte organisation.
     *
     * @param adminEmail Email de l'administrateur
     * @param organizationName Nom de l'organisation
     * @param organizationAddress Adresse de l'organisation
     */
    public void sendNewOrganizationNotificationEmail(
            String adminEmail,
            String organizationName,
            String organizationAddress) {
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            log.warn("EMAIL_ADMIN_HSCODE n'est pas configuré, notification admin non envoyée");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String from = fromEmail != null ? fromEmail : "noreply@enclume-numerique.com";
            String fromNameValue = fromName != null ? fromName : "Enclume Numérique";
            
            helper.setFrom(from, fromNameValue);
            helper.setTo(adminEmail);
            helper.setSubject("Nouvelle création de compte - " + (organizationName != null ? organizationName : "Organisation"));
            
            // Créer le contenu HTML simple
            String htmlContent = buildNewOrganizationNotificationHtml(organizationName, organizationAddress);
            if (htmlContent != null && !htmlContent.trim().isEmpty()) {
                helper.setText(htmlContent, true);
            } else {
                // Fallback en cas de problème avec le HTML
                String plainText = String.format(
                    "Une nouvelle organisation a été créée sur la plateforme HS-code.\n\n" +
                    "Nom de l'entreprise : %s\n" +
                    "Adresse : %s",
                    organizationName != null ? organizationName : "Non renseigné",
                    organizationAddress != null ? organizationAddress : "Non renseignée"
                );
                helper.setText(plainText, false);
            }

            mailSender.send(message);
            log.info("Notification admin envoyée à {} pour la nouvelle organisation {}", adminEmail, organizationName);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de la notification admin à {}: {}", adminEmail, e.getMessage(), e);
            // Ne pas faire échouer la création d'organisation si l'email admin échoue
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'envoi de la notification admin à {}: {}", adminEmail, e.getMessage(), e);
            // Ne pas faire échouer la création d'organisation si l'email admin échoue
        }
    }
    
    /**
     * Construit le contenu HTML de la notification admin pour une nouvelle organisation.
     */
    private String buildNewOrganizationNotificationHtml(String organizationName, String organizationAddress) {
        String safeName = organizationName != null ? organizationName : "Non renseigné";
        String safeAddress = organizationAddress != null ? organizationAddress : "Non renseignée";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #1e3c72; color: white; padding: 20px; text-align: center; }");
        html.append(".content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }");
        html.append(".info { background-color: white; padding: 15px; margin: 10px 0; border-left: 4px solid #1e3c72; }");
        html.append(".label { font-weight: bold; color: #1e3c72; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\">");
        html.append("<h1>Nouvelle création de compte</h1>");
        html.append("</div>");
        html.append("<div class=\"content\">");
        html.append("<p>Une nouvelle organisation a été créée sur la plateforme HS-code.</p>");
        html.append("<div class=\"info\">");
        html.append("<p><span class=\"label\">Nom de l'entreprise :</span> ").append(safeName).append("</p>");
        html.append("<p><span class=\"label\">Adresse :</span> ").append(safeAddress).append("</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        return html.toString();
    }

    /**
     * Récupère l'URL du frontend depuis les variables d'environnement ou utilise une valeur par défaut.
     */
    private String getFrontendUrl() {
        return frontendUrl != null ? frontendUrl : "https://hscode.enclume-numerique.com";
    }
}

