package com.muhend.backend.organization.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour QuotaExceededException - Phase 4 MVP : Quotas Basiques
 */
class QuotaExceededExceptionTest {

    @Test
    void testQuotaExceededException_WithMessage() {
        String message = "Quota mensuel dépassé pour l'organisation 'Test' (ID: 1). Utilisation: 100/50 requêtes";
        QuotaExceededException exception = new QuotaExceededException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testQuotaExceededException_WithMessageAndCause() {
        String message = "Quota mensuel dépassé";
        Throwable cause = new RuntimeException("Cause test");
        QuotaExceededException exception = new QuotaExceededException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}

