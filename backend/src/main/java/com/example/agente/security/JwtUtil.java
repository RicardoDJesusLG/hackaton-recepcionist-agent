package com.example.agente.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    // Clave de firma por defecto (debe tener al menos 256 bits / 32 bytes)
    private static final String DEFAULT_SECRET = "EstaEsUnaClaveSecretaMuyLargaYSeguraParaElHackathon2026!";
    
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret:}") String secret,
                   @Value("${jwt.expiration:86400000}") long expirationMs) {
        String finalSecret = (secret == null || secret.trim().length() < 32) ? DEFAULT_SECRET : secret;
        this.signingKey = Keys.hmacShaKeyFor(finalSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT para un propietario de negocio, incluyendo su empresaId en los claims.
     */
    public String generarToken(String username, UUID empresaId) {
        return Jwts.builder()
                .subject(username)
                .claim("empresaId", empresaId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extrae el username (subject) del token.
     */
    public String obtenerUsername(String token) {
        return obtenerClaims(token).getSubject();
    }

    /**
     * Extrae el empresaId de los claims del token.
     */
    public UUID obtenerEmpresaId(String token) {
        String idStr = obtenerClaims(token).get("empresaId", String.class);
        return idStr != null ? UUID.fromString(idStr) : null;
    }

    /**
     * Valida si el token ha expirado.
     */
    public boolean validarToken(String token) {
        try {
            return obtenerClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
