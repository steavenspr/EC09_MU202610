package com.example.auth.exception;

/**
 * Exception lancée quand un compte est temporairement bloqué
 * suite à trop de tentatives de connexion échouées.
 */
public class AccountLockedException extends RuntimeException {

    /**
     * Constructeur avec message d'erreur.
     *
     * @param message le message décrivant la raison du blocage
     */
    public AccountLockedException(String message) {
        super(message);
    }
}