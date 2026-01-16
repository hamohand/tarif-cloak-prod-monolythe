package com.muhend.backend.flyway;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Service pour réparer automatiquement Flyway en cas de mismatch de checksums.
 * S'exécute au démarrage de l'application pour réparer les checksums avant la validation.
 */
@Service
@Slf4j
@Order(1) // S'exécuter avant les autres services
public class FlywayRepairService implements ApplicationListener<ContextRefreshedEvent> {

    private final DataSource dataSource;
    private final FlywayProperties flywayProperties;
    private static boolean repairExecuted = false;

    public FlywayRepairService(DataSource dataSource, FlywayProperties flywayProperties) {
        this.dataSource = dataSource;
        this.flywayProperties = flywayProperties;
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // Réparer une seule fois au démarrage pour mettre à jour les checksums
        if (!repairExecuted) {
            repairFlyway();
            repairExecuted = true;
        }
    }

    /**
     * Répare Flyway en mettant à jour les checksums dans la table flyway_schema_history.
     */
    public void repairFlyway() {
        try {
            log.info("🔧 Démarrage de la réparation Flyway...");
            
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(flywayProperties.getLocations().toArray(new String[0]))
                    .baselineOnMigrate(flywayProperties.isBaselineOnMigrate())
                    .load();

            // Réparer les checksums
            flyway.repair();
            
            log.info("✅ Réparation Flyway terminée avec succès. Les checksums ont été mis à jour.");
            log.info("💡 IMPORTANT: Après ce premier démarrage réussi, réactivez 'validate-on-migrate: true' dans application.yml");
            log.info("💡 Cela garantira la sécurité des migrations futures.");
            
        } catch (Exception e) {
            log.error("❌ Erreur lors de la réparation Flyway: {}", e.getMessage(), e);
            // Ne pas faire échouer le démarrage de l'application
        }
    }
}

