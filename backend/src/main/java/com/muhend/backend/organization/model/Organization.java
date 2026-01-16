package com.muhend.backend.organization.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entité représentant une entreprise/organisation.
 * Phase 2 MVP : Association Utilisateur → Entreprise
 */
@Entity
@Table(name = "organization")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email; // Email de l'organisation (identifiant unique)
    
    @Column(name = "address", nullable = false, length = 512)
    private String address; // Adresse complète de l'organisation
    
    @Column(name = "activity_domain", nullable = true, length = 255)
    private String activityDomain; // Domaine d'activité de l'organisation
    
    @Column(name = "country", nullable = false, length = 2)
    private String country; // Code pays ISO-3166 alpha-2
    
    @Column(name = "phone", nullable = false, length = 32)
    private String phone; // Numéro de téléphone international (E.164)
    
    @Column(name = "keycloak_user_id", nullable = true, unique = true, length = 255)
    private String keycloakUserId; // Identifiant Keycloak du compte organisation
    
    @Column(name = "monthly_quota", nullable = true)
    private Integer monthlyQuota; // null = quota illimité (peut être défini par le plan tarifaire)
    
    @Column(name = "pricing_plan_id", nullable = true)
    private Long pricingPlanId; // Référence au plan tarifaire
    
    @Column(name = "trial_expires_at", nullable = true)
    private LocalDateTime trialExpiresAt; // Date d'expiration du plan d'essai (null si pas un plan d'essai)
    
    @Column(name = "trial_permanently_expired", nullable = false)
    private Boolean trialPermanentlyExpired = false; // true si l'essai est définitivement terminé (ne peut plus être réactivé)
    
    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId; // ID du client Stripe (pour éviter de créer plusieurs clients)
    
    @Column(name = "market_version", length = 10)
    private String marketVersion; // Version du marché (ex: DEFAULT, DZ) - référence à market_profile
    
    @Column(name = "monthly_plan_start_date", nullable = true)
    private LocalDate monthlyPlanStartDate; // Date de début du cycle mensuel actuel (du jour J au jour J-1 du mois suivant inclus)
    
    @Column(name = "monthly_plan_end_date", nullable = true)
    private LocalDate monthlyPlanEndDate; // Date de fin du cycle mensuel (inclus, réinitialisation le jour suivant)
    
    @Column(name = "pending_monthly_plan_id", nullable = true)
    private Long pendingMonthlyPlanId; // Plan mensuel en attente (prendra effet à la fin du cycle en cours)
    
    @Column(name = "pending_monthly_plan_change_date", nullable = true)
    private LocalDate pendingMonthlyPlanChangeDate; // Date à laquelle le changement de plan prendra effet
    
    @Column(name = "last_pay_per_request_invoice_date", nullable = true)
    private LocalDate lastPayPerRequestInvoiceDate; // Date de la dernière facture pay-per-request (pour facturer depuis cette date lors du passage vers mensuel)
    
    @Column(name = "pending_pay_per_request_plan_id", nullable = true)
    private Long pendingPayPerRequestPlanId; // Plan Pay-per-Request en attente (prendra effet si quota dépassé immédiatement, sinon à la fin du cycle mensuel)
    
    @Column(name = "pending_pay_per_request_change_date", nullable = true)
    private LocalDate pendingPayPerRequestChangeDate; // Date à laquelle le changement vers Pay-per-Request prendra effet (fin du cycle si quota non dépassé)

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true; // false = organisation désactivée (aucun collaborateur ne peut utiliser l'application)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

