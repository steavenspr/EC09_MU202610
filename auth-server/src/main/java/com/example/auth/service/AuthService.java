package com.example.auth.service;

import com.example.auth.entity.AuthNonce;
import com.example.auth.entity.User;
import com.example.auth.exception.AccountLockedException;
import com.example.auth.exception.AuthenticationFailedException;
import com.example.auth.exception.InvalidInputException;
import com.example.auth.exception.ResourceConflictException;
import com.example.auth.repository.AuthNonceRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.AesEncryptionService;
import com.example.auth.security.HmacService;
import com.example.auth.security.JwtService;
import com.example.auth.security.PasswordPolicyValidator;
import com.example.auth.dto.ChangePasswordRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Service principal gérant la logique d'authentification.
 * <p>
 * TP3 — Authentification forte par protocole HMAC signé.
 * Le mot de passe ne circule plus sur le réseau : le client prouve qu'il
 * connaît le secret en calculant une signature HMAC.
 * Protection anti-rejeu par nonce unique et fenêtre timestamp de ±60 secondes.
 * </p>
 * <p>
 * EC06 — Les tokens d'accès sont désormais des JWT signés (HS256) émis par
 * {@link JwtService}. Ils sont stateless : aucune persistance côté auth-server,
 * et sont validés indépendamment par le back Laravel via le même secret.
 * </p>
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final long TIMESTAMP_WINDOW_SECONDS = 60;
    private static final long NONCE_TTL_SECONDS = 120;
    private static final Set<String> ALLOWED_ROLES = Set.of(User.ROLE_APPRENANT, User.ROLE_FORMATEUR);

    private final UserRepository userRepository;
    private final AuthNonceRepository nonceRepository;
    private final AesEncryptionService aesEncryptionService;
    private final HmacService hmacService;
    private final JwtService jwtService;

    /**
     * Constructeur avec injection des dépendances.
     */
    public AuthService(UserRepository userRepository,
                       AuthNonceRepository nonceRepository,
                       AesEncryptionService aesEncryptionService,
                       HmacService hmacService,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.nonceRepository = nonceRepository;
        this.aesEncryptionService = aesEncryptionService;
        this.hmacService = hmacService;
        this.jwtService = jwtService;
    }

    /**
     * Nettoie une chaîne pour éviter l'injection dans les logs.
     */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\r\n]", "");
    }

    /**
     * Inscrit un nouvel utilisateur avec le rôle apprenant par défaut.
     */
    public User register(String email, String password) {
        return register(email, password, User.ROLE_APPRENANT);
    }

    /**
     * Inscrit un nouvel utilisateur avec le rôle applicatif demandé.
     * Le mot de passe est chiffré avec AES pour permettre la vérification HMAC ultérieure.
     *
     * @param email    l'adresse email de l'utilisateur
     * @param password le mot de passe en clair
     * @param role     le rôle applicatif ("apprenant" ou "formateur")
     * @return l'utilisateur créé
     * @throws InvalidInputException     si l'email, le mot de passe ou le rôle est invalide
     * @throws ResourceConflictException si l'email existe déjà
     */
    public User register(String email, String password, String role) {
        if (email == null || email.isEmpty()) {
            logger.warn("Inscription échouée : email vide");
            throw new InvalidInputException("Email cannot be empty");
        }
        if (!email.contains("@")) {
            if (logger.isWarnEnabled()) {
                logger.warn("Inscription échouée : format email invalide pour {}", sanitize(email));
            }
            throw new InvalidInputException("Invalid email format");
        }

        String resolvedRole = (role == null || role.isBlank()) ? User.ROLE_APPRENANT : role;
        if (!ALLOWED_ROLES.contains(resolvedRole)) {
            throw new InvalidInputException("Invalid role: must be apprenant or formateur");
        }

        PasswordPolicyValidator.validate(password);

        if (userRepository.findByEmail(email).isPresent()) {
            if (logger.isWarnEnabled()) {
                logger.warn("Inscription échouée : email déjà existant pour {}", sanitize(email));
            }
            throw new ResourceConflictException("Email already exists");
        }

        String encryptedPassword = aesEncryptionService.encrypt(password);
        User user = new User(email, encryptedPassword, resolvedRole);
        userRepository.save(user);
        if (logger.isInfoEnabled()) {
            logger.info("Inscription réussie pour : {} (role={})", sanitize(email), resolvedRole);
        }
        return user;
    }

    /**
     * Authentifie un utilisateur via le protocole HMAC et retourne un JWT signé.
     * Vérifie dans l'ordre : email, timestamp, nonce, signature HMAC.
     * Le nonce est réservé immédiatement après vérification pour bloquer
     * tout rejeu simultané, puis marqué consommé après validation HMAC.
     *
     * @return un JWT HS256 avec les claims {sub, email, role, iss, iat, exp}
     * @throws AuthenticationFailedException si la vérification échoue
     * @throws AccountLockedException        si le compte est bloqué
     */
    public String login(String email, String nonce, long timestamp, String hmac) {
        User user = getUserOrFail(email);
        checkLockout(user, email);
        checkTimestampWindow(timestamp, email);
        checkNonceNotUsed(user, nonce, email);

        // Réserver le nonce immédiatement (consumed = false)
        AuthNonce authNonce = new AuthNonce(
                user, nonce,
                LocalDateTime.now().plusSeconds(NONCE_TTL_SECONDS)
        );
        authNonce.setConsumed(false);
        nonceRepository.save(authNonce);

        // Déchiffrer le mot de passe et recalculer le HMAC
        String passwordPlain = aesEncryptionService.decrypt(user.getPasswordEncrypted());
        String message = email + ":" + nonce + ":" + timestamp;
        String expectedHmac = hmacService.compute(passwordPlain, message);
        handleFailedHmac(user, expectedHmac, hmac);

        // Marquer le nonce comme consommé
        authNonce.setConsumed(true);
        nonceRepository.save(authNonce);

        user.setFailedAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);

        String jwt = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        if (logger.isInfoEnabled()) {
            logger.info("Connexion réussie pour : {}", sanitize(email));
        }
        return jwt;
    }

    private User getUserOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Connexion échouée : email inconnu {}", sanitize(email));
                    }
                    return new AuthenticationFailedException("Authentication failed");
                });
    }

    private void checkLockout(User user, String email) {
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            if (logger.isWarnEnabled()) {
                logger.warn("Connexion échouée : compte bloqué pour {}", sanitize(email));
            }
            throw new AccountLockedException("Account is locked. Please try again later.");
        }
    }

    private void checkTimestampWindow(long timestamp, String email) {
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > TIMESTAMP_WINDOW_SECONDS) {
            if (logger.isWarnEnabled()) {
                logger.warn("Connexion échouée : timestamp hors fenêtre pour {}", sanitize(email));
            }
            throw new AuthenticationFailedException("Authentication failed");
        }
    }

    private void checkNonceNotUsed(User user, String nonce, String email) {
        if (nonceRepository.findByUserAndNonce(user, nonce).isPresent()) {
            if (logger.isWarnEnabled()) {
                logger.warn("Connexion échouée : nonce déjà utilisé pour {}", sanitize(email));
            }
            throw new AuthenticationFailedException("Authentication failed");
        }
    }

    private void handleFailedHmac(User user, String expectedHmac, String hmac) {
        if (!hmacService.verifyConstantTime(expectedHmac, hmac)) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 5) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(2));
                userRepository.save(user);
                throw new AccountLockedException("Account is locked. Please try again later.");
            }
            userRepository.save(user);
            throw new AuthenticationFailedException("Authentication failed");
        }
    }

    /**
     * Vérifie un JWT et récupère l'utilisateur correspondant.
     * Le JWT est validé cryptographiquement (signature + expiration + issuer),
     * puis l'email extrait est résolu en base pour vérifier que l'utilisateur
     * existe toujours.
     *
     * @param token le JWT (sans préfixe "Bearer ")
     * @return l'utilisateur correspondant
     * @throws AuthenticationFailedException si le token est invalide, expiré
     *                                       ou si l'utilisateur n'existe plus
     */
    public User getUserByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationFailedException("Invalid token");
        }
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthenticationFailedException("Invalid token");
        }
        String email = claims.get("email", String.class);
        if (email == null) {
            throw new AuthenticationFailedException("Invalid token");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid token"));
    }

    /**
     * Change le mot de passe d'un utilisateur authentifié.
     * Vérifie l'ancien mot de passe, la confirmation, et la politique de sécurité.
     */
    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Changement MDP échoué : email inconnu {}", sanitize(request.getEmail()));
                    return new AuthenticationFailedException("Authentication failed");
                });

        String currentPassword = aesEncryptionService.decrypt(user.getPasswordEncrypted());
        if (!currentPassword.equals(request.getOldPassword())) {
            logger.warn("Changement MDP échoué : ancien mot de passe incorrect pour {}", sanitize(request.getEmail()));
            throw new AuthenticationFailedException("Authentication failed");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidInputException("Passwords do not match");
        }

        PasswordPolicyValidator.validate(request.getNewPassword());

        String encryptedNewPassword = aesEncryptionService.encrypt(request.getNewPassword());
        user.setPasswordEncrypted(encryptedNewPassword);
        userRepository.save(user);

        logger.info("Mot de passe changé avec succès pour : {}", sanitize(request.getEmail()));
    }
}
