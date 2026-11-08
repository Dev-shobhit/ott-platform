package com.ott.userauth.component;

import com.ott.userauth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessExpiryMinutes;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry-minutes}") long accessExpiryMinutes) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMinutes = accessExpiryMinutes;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + Duration.ofMinutes(accessExpiryMinutes).toMillis());
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .claim("tier", user.getTier())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .id(UUID.randomUUID().toString())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseEncryptedClaims(token).getPayload();
    }

    public long getAccessExpirySeconds() {
        return Duration.ofMinutes(accessExpiryMinutes).toSeconds();
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public long getRemainingSeconds(Claims claims) {
        return (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
    }
}
