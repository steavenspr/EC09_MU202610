package com.example.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service de chiffrement AES symétrique.
 * Utilise AES/GCM/NoPadding avec une Server Master Key (SMK).
 * Le mot de passe est chiffré de façon réversible pour permettre
 * au serveur de recalculer le HMAC lors de l'authentification.
 *
 * Limite pédagogique : en production on éviterait le chiffrement
 * réversible — on préférerait un hash non réversible et adaptatif.
 */
@Service
public class AesEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12; // GCM recommande 12 octets
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec secretKey;

    /**
     * Constructeur — initialise la clé AES depuis la variable d'environnement APP_MASTER_KEY.
     * La clé est injectée par Spring via @Value, ce qui permet de la lire depuis :
     * - La variable d'environnement OS en production
     * - Le fichier application.properties en test
     * - Les secrets GitHub Actions en CI
     *
     * La clé doit faire exactement 32 caractères (256 bits).
     *
     * Limite pédagogique : en production on utiliserait un hash non réversible.
     *
     * @param masterKey la clé maître injectée par Spring
     * @throws IllegalStateException si la clé n'est pas définie ou trop courte
     */
    public AesEncryptionService(@Value("${APP_MASTER_KEY}") String masterKey) {
        if (masterKey == null || masterKey.length() < 32) {
            throw new IllegalStateException(
                    "La variable d'environnement APP_MASTER_KEY doit être définie et contenir au moins 32 caractères."
            );
        }
        byte[] keyBytes = masterKey.getBytes(StandardCharsets.UTF_8);
        byte[] key32 = new byte[32];
        System.arraycopy(keyBytes, 0, key32, 0, 32);
        this.secretKey = new SecretKeySpec(key32, "AES");
    }

    /**
     * Exception personnalisée pour les erreurs de chiffrement AES.
     */
    public static class AesEncryptionException extends RuntimeException {
        public AesEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Chiffre une chaîne en clair avec AES.
     * L'IV aléatoire est préfixé au résultat chiffré.
     *
     * Limite pédagogique : en production on éviterait le chiffrement réversible.
     *
     * @param plainText le texte en clair à chiffrer
     * @return Base64(IV + données chiffrées)
     */
    public String encrypt(String plainText) {
        try {
            byte[] iv = generateIv();
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, gcmSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (javax.crypto.NoSuchPaddingException |
                 java.security.NoSuchAlgorithmException |
                 java.security.InvalidKeyException |
                 javax.crypto.IllegalBlockSizeException |
                 javax.crypto.BadPaddingException |
                 java.security.InvalidAlgorithmParameterException e) {
            throw new AesEncryptionException("Erreur de chiffrement AES", e);
        }
    }

    /**
     * Déchiffre une chaîne chiffrée avec AES.
     *
     * @param encryptedText Base64(IV + données chiffrées)
     * @return le texte en clair
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            Cipher cipher = createCipher(Cipher.DECRYPT_MODE, gcmSpec);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (javax.crypto.NoSuchPaddingException |
                 java.security.NoSuchAlgorithmException |
                 java.security.InvalidKeyException |
                 javax.crypto.IllegalBlockSizeException |
                 javax.crypto.BadPaddingException |
                 java.security.InvalidAlgorithmParameterException e) {
            throw new AesEncryptionException("Erreur de déchiffrement AES", e);
        }
    }

    /**
     * Génère un IV aléatoire de la taille requise.
     */
    private byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Crée et initialise un Cipher pour le mode donné et le paramètre GCM.
     */
    private Cipher createCipher(int mode, GCMParameterSpec gcmSpec)
            throws javax.crypto.NoSuchPaddingException, java.security.NoSuchAlgorithmException,
            java.security.InvalidKeyException, java.security.InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(mode, secretKey, gcmSpec);
        return cipher;
    }
}