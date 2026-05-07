package com.example.auth.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Service de calcul et vérification HMAC-SHA256.
 * Utilisé pour prouver qu'un client connaît un secret sans l'envoyer.
 * La comparaison se fait en temps constant pour éviter les attaques timing.
 */
@Service
public class HmacService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Calcule un HMAC-SHA256.
     *
     * @param secret  la clé secrète (mot de passe en clair)
     * @param message le message à signer (email:nonce:timestamp)
     * @return la signature hexadécimale
     */
    public String compute(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur de calcul HMAC", e);
        }
    }

    /**
     * Compare deux signatures HMAC en temps constant.
     * Empêche les attaques timing qui exploitent les différences de durée
     * selon le nombre de caractères identiques.
     *
     * @param expected la signature attendue
     * @param actual   la signature reçue
     * @return true si les signatures sont identiques
     */
    public boolean verifyConstantTime(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}