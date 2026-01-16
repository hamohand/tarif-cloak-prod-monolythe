package com.muhend.backend.pricing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration pour s'assurer que la table pricing_plan a les bonnes contraintes.
 * S'exécute après que l'application soit complètement démarrée et que Hibernate ait initialisé le schéma.
 */
@Component
@Slf4j
@Order(1)
public class PricingPlanSchemaFix {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fixPricingPlanSchema() {
        try {
            log.info("Vérification et correction du schéma de la table pricing_plan...");
            
            // Vérifier si la table existe
            String checkTableExists = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_name = 'pricing_plan'
                )
                """;
            
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableExists, Boolean.class);
            if (tableExists == null || !tableExists) {
                log.warn("Table pricing_plan n'existe pas encore, elle sera créée par Hibernate");
                return;
            }
            
            // Vérifier et corriger price_per_month
            String checkNullable = """
                SELECT is_nullable 
                FROM information_schema.columns 
                WHERE table_name = 'pricing_plan' 
                AND column_name = 'price_per_month'
                """;
            
            try {
                String isNullable = jdbcTemplate.queryForObject(checkNullable, String.class);
                
                if (isNullable != null && "NO".equals(isNullable)) {
                    log.info("La colonne price_per_month n'est pas nullable, correction en cours...");
                    jdbcTemplate.execute("ALTER TABLE pricing_plan ALTER COLUMN price_per_month DROP NOT NULL");
                    log.info("✓ Colonne price_per_month corrigée : maintenant nullable");
                } else {
                    log.debug("Colonne price_per_month est déjà nullable");
                }
            } catch (Exception e) {
                log.warn("Impossible de vérifier price_per_month: {}", e.getMessage());
            }
            
            // Vérifier et corriger price_per_request
            try {
                String checkPricePerRequest = """
                    SELECT is_nullable 
                    FROM information_schema.columns 
                    WHERE table_name = 'pricing_plan' 
                    AND column_name = 'price_per_request'
                    """;
                
                String isPricePerRequestNullable = jdbcTemplate.queryForObject(checkPricePerRequest, String.class);
                if (isPricePerRequestNullable != null && "NO".equals(isPricePerRequestNullable)) {
                    log.info("Correction de price_per_request...");
                    jdbcTemplate.execute("ALTER TABLE pricing_plan ALTER COLUMN price_per_request DROP NOT NULL");
                    log.info("✓ Colonne price_per_request corrigée : maintenant nullable");
                }
            } catch (Exception e) {
                log.debug("Colonne price_per_request n'existe pas encore ou est déjà nullable");
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de la correction du schéma pricing_plan: {}", e.getMessage(), e);
            // Ne pas faire échouer le démarrage si la correction échoue
        }
    }
}

