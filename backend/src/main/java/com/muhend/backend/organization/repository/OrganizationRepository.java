package com.muhend.backend.organization.repository;

import com.muhend.backend.organization.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    /**
     * Trouve une organisation par son nom (exact match).
     */
    Optional<Organization> findByName(String name);
    
    /**
     * Vérifie si une organisation existe avec ce nom.
     */
    boolean existsByName(String name);
    
    /**
     * Trouve une organisation par son email (exact match).
     */
    Optional<Organization> findByEmail(String email);
    
    /**
     * Vérifie si une organisation existe avec cet email.
     */
    boolean existsByEmail(String email);
    
    /**
     * Trouve une organisation par son ID de client Stripe.
     */
    java.util.Optional<Organization> findByStripeCustomerId(String stripeCustomerId);

    /**
     * Trouve une organisation par l'identifiant Keycloak de son compte principal.
     */
    Optional<Organization> findByKeycloakUserId(String keycloakUserId);
    
    /**
     * Trouve les organisations avec un changement de plan en attente dont la date d'effet est arrivée ou passée.
     */
    @Query("SELECT o FROM Organization o WHERE o.pendingMonthlyPlanChangeDate IS NOT NULL AND o.pendingMonthlyPlanChangeDate <= :date")
    List<Organization> findByPendingMonthlyPlanChangeDateLessThanEqual(@Param("date") LocalDate date);
    
    /**
     * Trouve les organisations dont le cycle mensuel est expiré (endDate < date).
     */
    @Query("SELECT o FROM Organization o WHERE o.monthlyPlanEndDate IS NOT NULL AND o.monthlyPlanEndDate < :date")
    List<Organization> findByMonthlyPlanEndDateLessThan(@Param("date") LocalDate date);
    
    /**
     * Trouve les organisations avec un changement vers Pay-per-Request en attente.
     */
    @Query("SELECT o FROM Organization o WHERE o.pendingPayPerRequestPlanId IS NOT NULL")
    List<Organization> findByPendingPayPerRequestPlanIdIsNotNull();
}

