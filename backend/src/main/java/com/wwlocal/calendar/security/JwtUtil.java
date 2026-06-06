package com.wwlocal.calendar.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET = "wwlocal-calendar-jwt-secret-key-2026-min-256bits";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24小时

    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public String generateToken(String userId, String username, String role) {
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of("username", username, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public Map<String, Object> parseToken(String token) {
        var claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Map.of(
                "userId", claims.getSubject(),
                "username", claims.get("username", String.class),
                "role", claims.get("role", String.class)
        );
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}