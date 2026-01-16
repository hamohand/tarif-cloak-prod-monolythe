package com.muhend.backend.pricing.repository;

import com.muhend.backend.pricing.model.QuoteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRequestRepository extends JpaRepository<QuoteRequest, Long> {
    List<QuoteRequest> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    List<QuoteRequest> findAllByOrderByCreatedAtDesc();
    List<QuoteRequest> findByStatusOrderByCreatedAtDesc(QuoteRequest.QuoteStatus status);
    
    /**
     * Supprime toutes les demandes de devis d'une organisation.
     */
    @Modifying
    @Query("DELETE FROM QuoteRequest q WHERE q.organizationId = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") Long organizationId);
}

