package com.muhend.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI pour forcer HTTPS dans les URLs générées
 * et configurer l'authentification Bearer JWT pour Swagger UI.
 * Nécessaire quand l'application est derrière un reverse proxy (Traefik).
 */
@Configuration
public class OpenApiConfig {

    @Value("${FRONTEND_URL:https://hscode.enclume-numerique.com}")
    private String frontendUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        // Construire l'URL de base pour l'API (ajouter /api si nécessaire)
        String apiBaseUrl = frontendUrl;
        if (!apiBaseUrl.endsWith("/api")) {
            // Si FRONTEND_URL ne contient pas /api, l'ajouter
            apiBaseUrl = apiBaseUrl + "/api";
        }
        
        // S'assurer que l'URL utilise HTTPS
        if (apiBaseUrl.startsWith("http://")) {
            apiBaseUrl = apiBaseUrl.replace("http://", "https://");
        }
        
        Server server = new Server();
        server.setUrl(apiBaseUrl);
        server.setDescription("Production server (HTTPS)");

        // Configuration du schéma de sécurité Bearer JWT
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Entrez votre token JWT obtenu depuis Keycloak. Format: Bearer {token}");

        return new OpenAPI()
                .servers(List.of(server))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth));
    }
}

