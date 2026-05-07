package com.example.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant un utilisateur dans la base de données.
 * TP3 : mot de passe chiffré AES (réversible) avec Server Master Key.
 * TP4 / EC06 : rôle applicatif ({@code apprenant} ou {@code formateur}) servant
 * à l'autorisation côté back Laravel via JWT. Les tokens d'accès sont désormais
 * des JWT signés (stateless) : on ne les stocke plus en base.
 */
@Entity
@Table(name = "users")
public class User {

    public static final String ROLE_APPRENANT = "apprenant";
    public static final String ROLE_FORMATEUR = "formateur";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(name = "password_encrypted")
    private String passwordEncrypted;

    @Column(name = "role", nullable = false)
    private String role = ROLE_APPRENANT;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int failedAttempts = 0;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    /**
     * Constructeur par défaut requis par JPA.
     */
    public User() {}

    /**
     * Constructeur principal avec rôle apprenant par défaut.
     *
     * @param email             l'adresse email de l'utilisateur
     * @param passwordEncrypted le mot de passe chiffré avec AES
     */
    public User(String email, String passwordEncrypted) {
        this(email, passwordEncrypted, ROLE_APPRENANT);
    }

    /**
     * Constructeur complet.
     *
     * @param email             l'adresse email de l'utilisateur
     * @param passwordEncrypted le mot de passe chiffré avec AES
     * @param role              le rôle applicatif ("apprenant" ou "formateur")
     */
    public User(String email, String passwordEncrypted, String role) {
        this.email = email;
        this.passwordEncrypted = passwordEncrypted;
        this.role = (role == null || role.isBlank()) ? ROLE_APPRENANT : role;
        this.createdAt = LocalDateTime.now();
        this.failedAttempts = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordEncrypted() { return passwordEncrypted; }
    public void setPasswordEncrypted(String passwordEncrypted) { this.passwordEncrypted = passwordEncrypted; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockUntil() { return lockUntil; }
    public void setLockUntil(LocalDateTime lockUntil) { this.lockUntil = lockUntil; }
}
