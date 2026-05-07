// AuthenticationFailedException.java
package com.example.auth.exception;

/**
 * Exception lancée lorsque l'authentification échoue.
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais
 * être utilisée en production.
 * </p>
 */
public class AuthenticationFailedException extends RuntimeException {
    /**
     * Constructeur avec message d'erreur.
     *
     * @param message le message décrivant l'erreur
     */
    public AuthenticationFailedException(String message) {
        super(message);
    }
}