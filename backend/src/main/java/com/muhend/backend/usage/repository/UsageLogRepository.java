package com.muhend.backend.usage.repository;

import com.muhend.backend.usage.model.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {
    
    /**
     * Récupère tous les logs d'un utilisateur.
     */
    List<UsageLog> findByKeycloakUserId(String keycloakUserId);
    
    /**
     * Récupère les logs entre deux dates.
     */
    List<UsageLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Récupère les logs d'un utilisateur entre deux dates.
     */
    List<UsageLog> findByKeycloakUserIdAndTimestampBetween(
        String keycloakUserId, 
        LocalDateTime start, 
        LocalDateTime end
    );
    
    /**
     * Compte le nombre de logs d'un utilisateur entre deux dates.
     */
    long countByKeycloakUserIdAndTimestampBetween(
        String keycloakUserId,
        LocalDateTime start,
        LocalDateTime end
    );
    
    /**
     * Récupère les logs d'une organisation entre deux dates.
     */
    List<UsageLog> findByOrganizationIdAndTimestampBetween(
        Long organizationId,
        LocalDateTime start,
        LocalDateTime end
    );
    
    /**
     * Compte le nombre de logs d'une organisation entre deux dates.
     */
    long countByOrganizationIdAndTimestampBetween(
        Long organizationId,
        LocalDateTime start,
        LocalDateTime end
    );
    
    /**
     * Supprime tous les logs d'un utilisateur.
     */
    long deleteByKeycloakUserId(String keycloakUserId);
    
    /**
     * Supprime tous les logs d'une organisation.
     */
    @Modifying
    @Query("DELETE FROM UsageLog u WHERE u.organizationId = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") Long organizationId);
}

