package com.muhend.backend.organization.repository;

import com.muhend.backend.organization.model.OrganizationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {
    
    /**
     * Trouve toutes les organisations d'un utilisateur.
     */
    List<OrganizationUser> findByKeycloakUserId(String keycloakUserId);
    
    /**
     * Trouve l'association utilisateur-organisation spécifique.
     */
    Optional<OrganizationUser> findByOrganizationIdAndKeycloakUserId(Long organizationId, String keycloakUserId);
    
    /**
     * Trouve tous les utilisateurs d'une organisation.
     */
    List<OrganizationUser> findByOrganizationId(Long organizationId);
    
    /**
     * Vérifie si un utilisateur appartient à une organisation.
     */
    boolean existsByOrganizationIdAndKeycloakUserId(Long organizationId, String keycloakUserId);
    
    /**
     * Supprime l'association utilisateur-organisation.
     */
    void deleteByOrganizationIdAndKeycloakUserId(Long organizationId, String keycloakUserId);
    
    /**
     * Supprime toutes les associations d'un utilisateur.
     */
    long deleteByKeycloakUserId(String keycloakUserId);
    
    /**
     * Supprime toutes les associations d'une organisation.
     */
    @Modifying
    @Query("DELETE FROM OrganizationUser ou WHERE ou.organization.id = :organizationId")
    int deleteByOrganizationId(@Param("organizationId") Long organizationId);
}

