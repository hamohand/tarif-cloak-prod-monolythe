package com.muhend.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${cors.allowed-origins:https://hscode.enclume-numerique.com,https://www.hscode.enclume-numerique.com,http://localhost:4200}")
    private String allowedOrigins;
    
    @Value("${keycloak.admin.client-id:backend-client}")
    private String clientId;

    // SecurityFilterChain pour les endpoints publics (sans authentification OAuth2)
    // Doit être vérifiée en premier (ordre 1)
    // NOTE: Traefik enlève le préfixe /api avec stripprefix, donc les URIs arrivent sans /api
    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/auth/**", "/public/**", "/webhooks/**", "/swagger-ui/**", "/v3/api-docs/**", "/health/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    // SecurityFilterChain pour les endpoints protégés (avec authentification OAuth2)
    // Vérifiée en second (ordre 2)
    @Bean
    @Order(2)
    public SecurityFilterChain protectedSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Les requêtes GET sur /pricing-plans sont publiques
                .requestMatchers(HttpMethod.GET, "/pricing-plans/**").permitAll()
                // Les requêtes GET sur /market-profiles sont publiques (lecture seule)
                .requestMatchers(HttpMethod.GET, "/market-profiles/**").permitAll()
                // Endpoint de recherche accessible aux utilisateurs avec le rôle USER ou ADMIN
                .requestMatchers("/recherche/**").hasAnyRole("USER", "ADMIN")
                // Tous les autres endpoints nécessitent une authentification
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Non autorisé\", \"message\": \"" + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\": \"Accès refusé\", \"message\": \"Vous n'avez pas les permissions nécessaires\"}");
                })
            );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // --- IMPORTANT ---
        // Utiliser setAllowedOriginPatterns au lieu de setAllowedOrigins quand allowCredentials est true
        // Les origines CORS sont configurées via la variable d'environnement cors.allowed-origins
        // Format: "origin1,origin2,origin3" (séparées par des virgules)
        List<String> origins = Arrays.asList(allowedOrigins.split("\\s*,\\s*"));
        configuration.setAllowedOriginPatterns(origins);

        // Méthodes HTTP autorisées (ajouter HEAD pour Swagger UI)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        
        // Headers autorisés (tous les headers sont autorisés)
        configuration.setAllowedHeaders(List.of("*"));
        
        configuration.setAllowCredentials(true);
        
        // Headers exposés (ajouter plus de headers pour Swagger UI)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "Content-Disposition",
            "X-Total-Count",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // Max age pour le cache des requêtes preflight (1 heure)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * Configure le convertisseur JWT pour extraire les rôles Keycloak
     * Les rôles peuvent être dans :
     * - realm_access.roles (rôles au niveau du realm)
     * - resource_access.{client-id}.roles (rôles au niveau du client)
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            
            // Extraire les rôles du realm
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                List<String> realmRoles = (List<String>) realmAccess.get("roles");
                if (realmRoles != null) {
                    authorities.addAll(realmRoles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList()));
                }
            }
            
            // Extraire les rôles du client
            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
                if (clientAccess != null) {
                    @SuppressWarnings("unchecked")
                    List<String> clientRoles = (List<String>) clientAccess.get("roles");
                    if (clientRoles != null) {
                        authorities.addAll(clientRoles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .collect(Collectors.toList()));
                    }
                }
            }
            
            return authorities;
        });
        
        return converter;
    }
}
