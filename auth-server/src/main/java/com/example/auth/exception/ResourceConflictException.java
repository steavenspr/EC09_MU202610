// ResourceConflictException.java
package com.example.auth.exception;

/**
 * Exception lancée lorsqu'une ressource existe déjà en base de données.
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais
 * être utilisée en production.
 * </p>
 */
public class ResourceConflictException extends RuntimeException {
    /**
     * Constructeur avec message d'erreur.
     *
     * @param message le message décrivant l'erreur
     */
    public ResourceConflictException(String message) {
        super(message);
    }
}