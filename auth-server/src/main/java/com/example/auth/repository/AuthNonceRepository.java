package com.example.auth.repository;

import com.example.auth.entity.AuthNonce;
import com.example.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository d'accès aux nonces d'authentification.
 */
public interface AuthNonceRepository extends JpaRepository<AuthNonce, Long> {

    /**
     * Recherche un nonce par utilisateur et valeur du nonce.
     *
     * @param user  l'utilisateur concerné
     * @param nonce la valeur du nonce
     * @return le nonce s'il existe
     */
    Optional<AuthNonce> findByUserAndNonce(User user, String nonce);
}