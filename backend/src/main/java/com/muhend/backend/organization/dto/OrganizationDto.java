package com.muhend.backend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour les opérations sur les organisations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {
    
    private Long id;
    private String name;
    private String email; // Email de contact de l'organisation
    private String address;
    private String activityDomain; // Domaine d'activité
    private String country;
    private String phone;
    private Integer monthlyQuota; // null = quota illimité
    private Long pricingPlanId; // ID du plan tarifaire
    private String marketVersion; // Version du marché (ex: DEFAULT, DZ)
    private LocalDateTime trialExpiresAt; // Date d'expiration du plan d'essai (null si pas un plan d'essai)
    private Boolean trialPermanentlyExpired; // true si l'essai est définitivement terminé (ne peut plus être réactivé)
    private LocalDate monthlyPlanStartDate; // Date de début du cycle mensuel actuel
    private LocalDate monthlyPlanEndDate; // Date de fin du cycle mensuel (inclus)
    private Long pendingMonthlyPlanId; // Plan mensuel en attente (prendra effet à la fin du cycle)
    private LocalDate pendingMonthlyPlanChangeDate; // Date à laquelle le changement prendra effet
    private LocalDate lastPayPerRequestInvoiceDate; // Date de la dernière facture pay-per-request
    private Long pendingPayPerRequestPlanId; // Plan Pay-per-Request en attente (prendra effet si quota dépassé immédiatement, sinon à la fin du cycle)
    private LocalDate pendingPayPerRequestChangeDate; // Date à laquelle le changement vers Pay-per-Request prendra effet
    private Boolean enabled; // false = organisation désactivée (aucun collaborateur ne peut utiliser l'application)
    private LocalDateTime createdAt;
    
    // Pour les réponses avec le nombre d'utilisateurs
    private Long userCount;
    
    // Pour les réponses avec l'utilisation du quota
    private Long currentMonthUsage; // Nombre de requêtes utilisées ce mois
}

