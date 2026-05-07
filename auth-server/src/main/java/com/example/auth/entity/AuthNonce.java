package com.example.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité représentant un nonce d'authentification.
 * Chaque nonce est unique par utilisateur et ne peut être utilisé qu'une seule fois.
 * TTL de base : 120 secondes.
 * Empêche les attaques par rejeu.
 */
@Entity
@Table(name = "auth_nonce",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "nonce"}))
public class AuthNonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nonce;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean consumed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Constructeur par défaut requis par JPA.
     */
    public AuthNonce() {}

    /**
     * Constructeur principal.
     *
     * @param user      l'utilisateur associé au nonce
     * @param nonce     la valeur unique du nonce (UUID)
     * @param expiresAt la date d'expiration du nonce
     */
    public AuthNonce(User user, String nonce, LocalDateTime expiresAt) {
        this.user = user;
        this.nonce = nonce;
        this.expiresAt = expiresAt;
        this.consumed = false;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}