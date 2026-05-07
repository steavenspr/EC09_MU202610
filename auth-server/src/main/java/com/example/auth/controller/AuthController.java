package com.example.auth.controller;

import com.example.auth.dto.ChangePasswordRequest;
import com.example.auth.dto.LoginRequest;
import com.example.auth.entity.User;
import com.example.auth.exception.AuthenticationFailedException;
import com.example.auth.security.JwtService;
import com.example.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Contrôleur REST gérant les endpoints d'authentification.
 * Le login accepte une preuve HMAC et retourne un JWT signé (HS256).
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * Santé du service (healthcheck Docker / supervision).
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * Inscrit un nouvel utilisateur.
     *
     * @param email    l'adresse email de l'utilisateur
     * @param password le mot de passe en clair
     * @param role     le rôle applicatif ("apprenant" par défaut, ou "formateur")
     * @return message de confirmation
     */
    @PostMapping("/auth/register")
    public String register(@RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(defaultValue = User.ROLE_APPRENANT) String role) {
        authService.register(email, password, role);
        return "User registered";
    }

    /**
     * Authentifie un utilisateur via le protocole HMAC.
     * Retourne un JWT signé (HS256) et son instant d'expiration.
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String jwt = authService.login(
                request.getEmail(),
                request.getNonce(),
                request.getTimestamp(),
                request.getHmac()
        );
        Instant expiresAt = Instant.now().plus(jwtService.getExpirationMinutes(), ChronoUnit.MINUTES);
        return ResponseEntity.ok(Map.of(
                "accessToken", jwt,
                "tokenType", "Bearer",
                "expiresAt", expiresAt.toString()
        ));
    }

    /**
     * Retourne les informations de l'utilisateur authentifié.
     * Le JWT doit être passé via le header {@code Authorization: Bearer <token>}.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = extractBearerToken(authorization);
        User user = authService.getUserByToken(token);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole()
        ));
    }

    /**
     * Change le mot de passe d'un utilisateur authentifié.
     */
    @PutMapping("/auth/change-password")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationFailedException("Invalid token");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
