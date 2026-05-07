// InvalidInputException.java
package com.example.auth.exception;

/**
 * Exception lancée lorsque les données saisies sont invalides.
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais
 * être utilisée en production.
 * </p>
 */
public class InvalidInputException extends RuntimeException {
    /**
     * Constructeur avec message d'erreur.
     *
     * @param message le message décrivant l'erreur
     */
    public InvalidInputException(String message) {
        super(message);
    }
}