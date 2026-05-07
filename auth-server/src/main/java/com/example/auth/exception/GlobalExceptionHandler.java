package com.example.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions HTTP.
 * Intercepte les exceptions métier et retourne des réponses JSON structurées.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les erreurs de saisie invalide.
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP entrante
     * @return réponse 400 Bad Request
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInput(
            InvalidInputException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère les échecs d'authentification.
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP entrante
     * @return réponse 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthFailed(
            AuthenticationFailedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère les conflits de ressources existantes.
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP entrante
     * @return réponse 409 Conflict
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ResourceConflictException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Gère les comptes temporairement bloqués.
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP entrante
     * @return réponse 423 Locked
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(
            AccountLockedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.LOCKED, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Construit une réponse d'erreur JSON standardisée.
     *
     * @param status  le statut HTTP à retourner
     * @param message le message d'erreur
     * @param path    le chemin de la requête concernée
     * @return la réponse structurée
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return new ResponseEntity<>(body, status);
    }
}