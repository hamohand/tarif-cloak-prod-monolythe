package com.muhend.backend.exception;

import com.muhend.backend.organization.exception.QuotaExceededException;
import com.muhend.backend.organization.exception.UserNotAssociatedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global pour l'application.
 * Gère les exceptions personnalisées et renvoie des réponses HTTP appropriées.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Gère les exceptions de quota dépassé.
     * Renvoie une réponse HTTP 429 (Too Many Requests) avec un message d'erreur.
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceededException(QuotaExceededException ex) {
        log.warn("Quota dépassé: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "QUOTA_EXCEEDED");
        response.put("message", ex.getMessage());
        response.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(response);
    }
    
    /**
     * Gère les exceptions lorsqu'un utilisateur n'est pas associé à une organisation.
     * Renvoie une réponse HTTP 403 (Forbidden) avec un message d'erreur.
     * Un utilisateur DOIT toujours être associé à une organisation dans cette application.
     */
    @ExceptionHandler(UserNotAssociatedException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotAssociatedException(UserNotAssociatedException ex) {
        log.warn("Utilisateur non associé à une organisation: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "USER_NOT_ASSOCIATED");
        response.put("message", "Vous devez être associé à une organisation pour accéder à cette ressource. " +
                                "Veuillez contacter un administrateur pour être associé à une organisation.");
        response.put("detail", ex.getMessage());
        response.put("status", HttpStatus.FORBIDDEN.value());
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
}

