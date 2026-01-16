package com.muhend.backend.organization.exception;

/**
 * Exception levée lorsque le quota mensuel d'une organisation est dépassé.
 * Phase 4 MVP : Quotas Basiques
 */
public class QuotaExceededException extends RuntimeException {
    
    public QuotaExceededException(String message) {
        super(message);
    }
    
    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

