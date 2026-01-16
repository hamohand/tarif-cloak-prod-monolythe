package com.muhend.backend.organization.exception;

/**
 * Exception levée lorsqu'un utilisateur n'est pas associé à une organisation.
 * Dans cette application, un utilisateur DOIT toujours être associé à une organisation.
 */
public class UserNotAssociatedException extends RuntimeException {
    
    public UserNotAssociatedException(String message) {
        super(message);
    }
    
    public UserNotAssociatedException(String userId, String message) {
        super(String.format("Utilisateur %s: %s", userId, message));
    }
    
    public UserNotAssociatedException(String message, Throwable cause) {
        super(message, cause);
    }
}

