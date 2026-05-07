package com.example.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Service d'émission et de vérification des JWT signés (HS256).
 *
 * Le JWT remplace le token UUID stocké en base :
 *  - il est stateless (rien à persister côté serveur),
 *  - il est auto-vérifiable par le back Laravel qui partage le même secret,
 *  - il contient les claims minimaux nécessaires à l'autorisation (id, email, rôle).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;
    private final String issuer;

    /**
     * Construit le service à partir des propriétés Spring.
     *
     * @param secret            secret HMAC (>= 32 caractères)
     * @param expirationMinutes durée de vie du token en minutes
     * @param issuer            émetteur déclaré dans le claim "iss"
     */
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-minutes:15}") long expirationMinutes,
                      @Value("${jwt.issuer:ec06-auth-server}") String issuer) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "La propriété jwt.secret (JWT_SECRET) doit être définie et contenir au moins 32 caractères."
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    /**
     * Émet un JWT signé contenant les claims utilisateur.
     *
     * @param userId identifiant interne de l'utilisateur
     * @param email  email de l'utilisateur
     * @param role   rôle applicatif (ex: "apprenant", "formateur")
     * @return la chaîne JWT compacte
     */
    public String generateToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Vérifie la signature et les dates d'un JWT et renvoie ses claims.
     *
     * @param token le JWT compact
     * @return les claims décodés
     * @throws JwtException si le token est invalide, expiré ou mal signé
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * @return la durée de vie du token en minutes (utile pour le champ expiresAt)
     */
    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
