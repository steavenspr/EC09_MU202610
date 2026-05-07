package com.example.auth.dto;

/**
 * DTO représentant la requête de login HMAC envoyée par le client.
 * Le mot de passe ne circule pas — seule la preuve HMAC est transmise.
 */
public class LoginRequest {
    private String email;
    private String nonce;
    private long timestamp;
    private String hmac;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getHmac() { return hmac; }
    public void setHmac(String hmac) { this.hmac = hmac; }
}