package com.muhend.backend.pricing.repository;

import com.muhend.backend.pricing.model.PricingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
    List<PricingPlan> findByIsActiveTrueOrderByDisplayOrderAsc();
    Optional<PricingPlan> findByIdAndIsActiveTrue(Long id);
    List<PricingPlan> findAllByOrderByDisplayOrderAsc();
    Optional<PricingPlan> findByName(String name);
    boolean existsByName(String name);
    
    // Méthodes pour filtrer par version de marché
    List<PricingPlan> findByMarketVersionAndIsActiveTrueOrderByDisplayOrderAsc(String marketVersion);
    List<PricingPlan> findByMarketVersionAndIsActiveTrueAndIsCustomFalseOrderByDisplayOrderAsc(String marketVersion);
    List<PricingPlan> findByOrganizationIdAndIsActiveTrueOrderByDisplayOrderAsc(Long organizationId);
}

