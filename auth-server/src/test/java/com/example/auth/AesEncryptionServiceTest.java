package com.example.auth;

import com.example.auth.security.AesEncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")

class AesEncryptionServiceTest {

    @Autowired
    private AesEncryptionService aesEncryptionService;

    @Test
    void testEncryptDecryptRoundTrip() {
        String plainText = "MonMotDePasse1!";
        String encrypted = aesEncryptionService.encrypt(plainText);
        String decrypted = aesEncryptionService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptedDifferentDuClair() {
        String plainText = "MonMotDePasse1!";
        String encrypted = aesEncryptionService.encrypt(plainText);
        assertNotEquals(plainText, encrypted);
    }

    @Test
    void testDeuxChiffrementsDifferents() {
        // IV aléatoire → deux chiffrements du même texte doivent être différents
        String plainText = "MonMotDePasse1!";
        String encrypted1 = aesEncryptionService.encrypt(plainText);
        String encrypted2 = aesEncryptionService.encrypt(plainText);
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void testDecryptTexteModifieEchoue() {
        String plainText = "MonMotDePasse1!";
        String encrypted = aesEncryptionService.encrypt(plainText);
        // Modifier le texte chiffré doit provoquer une exception
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "XXXX";
        assertThrows(AesEncryptionService.AesEncryptionException.class, () ->
                aesEncryptionService.decrypt(tampered));
    }

    @Test
    void testDemarrageKoSiMasterKeyAbsente() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new AesEncryptionService("courte"));
        assertTrue(ex.getMessage().contains("APP_MASTER_KEY"));
    }
}