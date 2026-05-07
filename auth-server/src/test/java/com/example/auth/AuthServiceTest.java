package com.example.auth;

import com.example.auth.dto.ChangePasswordRequest;
import com.example.auth.exception.AuthenticationFailedException;
import com.example.auth.exception.InvalidInputException;
import com.example.auth.exception.ResourceConflictException;
import com.example.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import com.example.auth.exception.AccountLockedException;

import com.example.auth.security.HmacService;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private HmacService hmacService;

    // Mot de passe valide selon la nouvelle politique TP2
    private static final String VALID_PASSWORD = "Motdepasse1!";

    @Test
    void testRegisterOK() {
        assertDoesNotThrow(() -> authService.register("test@example.com", VALID_PASSWORD));
    }

    @Test
    void testRegisterEmailDejaExistant() {
        authService.register("double@example.com", VALID_PASSWORD);
        assertThrows(ResourceConflictException.class, () ->
                authService.register("double@example.com", VALID_PASSWORD));
    }

    @Test
    void testRegisterMotDePasseTropCourt() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("test@example.com", "ab"));
    }

    @Test
    void testRegisterMotDePasseSansMajuscule() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("test@example.com", "motdepasse1!aa"));
    }

    @Test
    void testRegisterMotDePasseSansSpecial() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("test@example.com", "Motdepasse123"));
    }

    @Test
    void testLoginOK() {
        authService.register("login@example.com", VALID_PASSWORD);
        String email = "login@example.com";
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        assertDoesNotThrow(() -> authService.login(email, nonce, timestamp, hmac));
    }

    @Test
    void testLoginMauvaisMotDePasse() {
        authService.register("login2@example.com", VALID_PASSWORD);
        String email = "login2@example.com";
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String message = email + ":" + nonce + ":" + timestamp;
        // Mauvais mot de passe utilisé pour le HMAC
        String hmac = hmacService.compute("MauvaisMotDePasse1!", message);
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login(email, nonce, timestamp, hmac));
    }

    @Test
    void testLoginEmailInconnu() {
        String email = "inconnu@example.com";
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login(email, nonce, timestamp, hmac));
    }

    @Test
    void testRegisterEmailVide() {
        assertThrows(Exception.class, () ->
                authService.register("", VALID_PASSWORD));
    }

    @Test
    void testGetUserByTokenInvalide() {
        assertThrows(AuthenticationFailedException.class, () ->
                authService.getUserByToken("tokeninvalide"));
    }

    @Test
    void testLockoutApres5Echecs() {
        authService.register("lock@example.com", VALID_PASSWORD);
        String email = "lock@example.com";
        // 5 tentatives échouées avec mauvais mot de passe (donc mauvais HMAC)
        for (int i = 0; i < 5; i++) {
            String nonce = UUID.randomUUID().toString();
            long timestamp = Instant.now().getEpochSecond();
            String message = email + ":" + nonce + ":" + timestamp;
            String hmac = hmacService.compute("MauvaisMotDePasse1!", message);
            try {
                authService.login(email, nonce, timestamp, hmac);
            } catch (AuthenticationFailedException | AccountLockedException e) {
                // attendu
            }
        }
        // La 6ème tentative doit retourner AccountLockedException (avec bon mot de passe)
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        assertThrows(AccountLockedException.class, () ->
                authService.login(email, nonce, timestamp, hmac));
    }

    @Test
    void testNonDivulgationErreur() {
        authService.register("test2@example.com", VALID_PASSWORD);
        // Email inconnu
        String email1 = "inconnu2@example.com";
        String nonce1 = UUID.randomUUID().toString();
        long timestamp1 = Instant.now().getEpochSecond();
        String message1 = email1 + ":" + nonce1 + ":" + timestamp1;
        String hmac1 = hmacService.compute(VALID_PASSWORD, message1);
        AuthenticationFailedException ex1 = assertThrows(
                AuthenticationFailedException.class, () ->
                        authService.login(email1, nonce1, timestamp1, hmac1));
        // Mauvais mot de passe
        String email2 = "test2@example.com";
        String nonce2 = UUID.randomUUID().toString();
        long timestamp2 = Instant.now().getEpochSecond();
        String message2 = email2 + ":" + nonce2 + ":" + timestamp2;
        String hmac2 = hmacService.compute("MauvaisMotDePasse1!", message2);
        AuthenticationFailedException ex2 = assertThrows(
                AuthenticationFailedException.class, () ->
                        authService.login(email2, nonce2, timestamp2, hmac2));
        // Les deux messages doivent être identiques
        assertEquals(ex1.getMessage(), ex2.getMessage());
    }

    @Test
    void testRegisterEmailSansArobase() {
        assertThrows(InvalidInputException.class, () ->
                authService.register("emailsansarobase", VALID_PASSWORD));
    }

    @Test
    void testLoginRetourneToken() {
        authService.register("token@example.com", VALID_PASSWORD);
        String email = "token@example.com";
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        String token = authService.login(email, nonce, timestamp, hmac);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGetUserByTokenValide() {
        authService.register("valid@example.com", VALID_PASSWORD);
        String email = "valid@example.com";
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        String token = authService.login(email, nonce, timestamp, hmac);
        assertDoesNotThrow(() -> authService.getUserByToken(token));
    }

    @Test
    void testTimestampExpire() {
        authService.register("expire@example.com", VALID_PASSWORD);
        String email = "expire@example.com";
        String nonce = UUID.randomUUID().toString();
        // Timestamp vieux de 120 secondes — hors fenêtre des 60s
        long timestamp = Instant.now().getEpochSecond() - 120;
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login(email, nonce, timestamp, hmac));
    }

    @Test
    void testTimestampFutur() {
        authService.register("futur@example.com", VALID_PASSWORD);
        String email = "futur@example.com";
        String nonce = UUID.randomUUID().toString();
        // Timestamp dans 120 secondes — hors fenêtre des 60s
        long timestamp = Instant.now().getEpochSecond() + 120;
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login(email, nonce, timestamp, hmac));
    }

    @Test
    void testNonceDejaUtilise() {
        authService.register("nonce@example.com", VALID_PASSWORD);
        String email = "nonce@example.com";
        String nonce = UUID.randomUUID().toString();
        long timestamp = Instant.now().getEpochSecond();
        String message = email + ":" + nonce + ":" + timestamp;
        String hmac = hmacService.compute(VALID_PASSWORD, message);
        // Première connexion — doit réussir
        authService.login(email, nonce, timestamp, hmac);
        // Deuxième connexion avec le même nonce — doit échouer
        assertThrows(AuthenticationFailedException.class, () ->
                authService.login(email, nonce, timestamp, hmac));
    }

    @Test
    void testChangementMotDePasseReussi() {
        authService.register("change@example.com", VALID_PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail("change@example.com");
        request.setOldPassword(VALID_PASSWORD);
        request.setNewPassword("NouveauMdp1!");
        request.setConfirmPassword("NouveauMdp1!");
        assertDoesNotThrow(() -> authService.changePassword(request));
    }

    @Test
    void testChangementMotDePasseAncienIncorrect() {
        authService.register("change2@example.com", VALID_PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail("change2@example.com");
        request.setOldPassword("MauvaisAncienMdp1!");
        request.setNewPassword("NouveauMdp1!");
        request.setConfirmPassword("NouveauMdp1!");
        assertThrows(AuthenticationFailedException.class, () ->
                authService.changePassword(request));
    }

    @Test
    void testChangementMotDePasseConfirmationDifferente() {
        authService.register("change3@example.com", VALID_PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail("change3@example.com");
        request.setOldPassword(VALID_PASSWORD);
        request.setNewPassword("NouveauMdp1!");
        request.setConfirmPassword("ConfirmDifferente1!");
        assertThrows(InvalidInputException.class, () ->
                authService.changePassword(request));
    }

    @Test
    void testChangementMotDePasseTropFaible() {
        authService.register("change4@example.com", VALID_PASSWORD);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail("change4@example.com");
        request.setOldPassword(VALID_PASSWORD);
        request.setNewPassword("faible");
        request.setConfirmPassword("faible");
        assertThrows(InvalidInputException.class, () ->
                authService.changePassword(request));
    }

    @Test
    void testChangementMotDePasseUtilisateurInexistant() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setEmail("inexistant@example.com");
        request.setOldPassword(VALID_PASSWORD);
        request.setNewPassword("NouveauMdp1!");
        request.setConfirmPassword("NouveauMdp1!");
        assertThrows(AuthenticationFailedException.class, () ->
                authService.changePassword(request));
    }
}