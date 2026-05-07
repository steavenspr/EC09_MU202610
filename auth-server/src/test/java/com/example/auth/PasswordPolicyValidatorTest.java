package com.example.auth;

import com.example.auth.exception.InvalidInputException;
import com.example.auth.security.PasswordPolicyValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyValidatorTest {

    @Test
    void testMotDePasseValide() {
        assertDoesNotThrow(() -> PasswordPolicyValidator.validate("Motdepasse1!"));
    }

    @Test
    void testMotDePasseTropCourt() {
        assertThrows(InvalidInputException.class, () ->
                PasswordPolicyValidator.validate("Court1!"));
    }

    @Test
    void testMotDePasseSansMajuscule() {
        assertThrows(InvalidInputException.class, () ->
                PasswordPolicyValidator.validate("motdepasse1!aa"));
    }

    @Test
    void testMotDePasseSansMinuscule() {
        assertThrows(InvalidInputException.class, () ->
                PasswordPolicyValidator.validate("MOTDEPASSE1!AA"));
    }

    @Test
    void testMotDePasseSansChiffre() {
        assertThrows(InvalidInputException.class, () ->
                PasswordPolicyValidator.validate("Motdepasselong!"));
    }

    @Test
    void testMotDePasseSansCaractereSpecial() {
        assertThrows(InvalidInputException.class, () ->
                PasswordPolicyValidator.validate("Motdepasse123"));
    }

    @Test
    void testMotDePasseNull() {
        assertThrows(InvalidInputException.class, () ->
                PasswordPolicyValidator.validate(null));
    }
}