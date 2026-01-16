package com.muhend.backend.market.repository;

import com.muhend.backend.market.model.MarketProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour gérer les profils de marché.
 */
@Repository
public interface MarketProfileRepository extends JpaRepository<MarketProfile, Long> {
    
    /**
     * Trouve un profil par sa version de marché.
     */
    Optional<MarketProfile> findByMarketVersion(String marketVersion);
    
    /**
     * Trouve un profil par son code ISO alpha-2.
     */
    Optional<MarketProfile> findByCountryCodeIsoAlpha2(String countryCodeIsoAlpha2);
    
    /**
     * Récupère tous les profils actifs, triés par ordre d'affichage.
     */
    List<MarketProfile> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Vérifie si un profil existe pour une version de marché donnée.
     */
    boolean existsByMarketVersion(String marketVersion);
    
    /**
     * Vérifie si un profil existe pour un code ISO alpha-2 donné.
     */
    boolean existsByCountryCodeIsoAlpha2(String countryCodeIsoAlpha2);
}

