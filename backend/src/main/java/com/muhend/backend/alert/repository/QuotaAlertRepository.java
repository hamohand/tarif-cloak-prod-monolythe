package com.muhend.backend.alert.repository;

import com.muhend.backend.alert.model.QuotaAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuotaAlertRepository extends JpaRepository<QuotaAlert, Long> {
    
    /**
     * Récupère les alertes non lues pour une organisation.
     */
    List<QuotaAlert> findByOrganizationIdAndIsReadFalseOrderByCreatedAtDesc(Long organizationId);
    
    /**
     * Récupère toutes les alertes non lues.
     */
    List<QuotaAlert> findByIsReadFalseOrderByCreatedAtDesc();
    
    /**
     * Récupère les alertes d'une organisation.
     */
    List<QuotaAlert> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    
    /**
     * Marque une alerte comme lue.
     */
    @Modifying
    @Query("UPDATE QuotaAlert a SET a.isRead = true WHERE a.id = :id")
    void markAsRead(@Param("id") Long id);
    
    /**
     * Marque toutes les alertes d'une organisation comme lues.
     */
    @Modifying
    @Query("UPDATE QuotaAlert a SET a.isRead = true WHERE a.organizationId = :organizationId")
    void markAllAsReadForOrganization(@Param("organizationId") Long organizationId);
    
    /**
     * Supprime les alertes anciennes (plus de 30 jours).
     */
    @Modifying
    @Query("DELETE FROM QuotaAlert a WHERE a.createdAt < :cutoffDate")
    void deleteOldAlerts(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Compte les alertes non lues pour une organisation.
     */
    long countByOrganizationIdAndIsReadFalse(Long organizationId);
    
    /**
     * Compte toutes les alertes non lues.
     */
    long countByIsReadFalse();
    
    /**
     * Supprime toutes les alertes d'une organisation.
     */
    @Modifying
    @Query("DELETE FROM QuotaAlert q WHERE q.organizationId = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") Long organizationId);
}

