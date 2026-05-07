package com.example.auth.security;

import com.example.auth.exception.InvalidInputException;

/**
 * Validateur de politique de mot de passe.
 * Vérifie que le mot de passe respecte les règles de sécurité minimales du TP2.
 * TP2 améliore le stockage mais ne protège pas encore contre le rejeu.
 */
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 12;

    // Classe utilitaire — pas d'instanciation
    private PasswordPolicyValidator() {}

    /**
     * Valide le mot de passe selon la politique de sécurité.
     * Lance une InvalidInputException si le mot de passe ne respecte pas les règles.
     *
     * @param password le mot de passe à valider
     * @throws InvalidInputException si le mot de passe est invalide
     */
    public static void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new InvalidInputException(
                "Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new InvalidInputException(
                "Le mot de passe doit contenir au moins une majuscule");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            throw new InvalidInputException(
                "Le mot de passe doit contenir au moins une minuscule");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new InvalidInputException(
                "Le mot de passe doit contenir au moins un chiffre");
        }
        if (!password.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;':\",./<>?".indexOf(c) >= 0)) {
            throw new InvalidInputException(
                "Le mot de passe doit contenir au moins un caractère spécial");
        }
    }
}