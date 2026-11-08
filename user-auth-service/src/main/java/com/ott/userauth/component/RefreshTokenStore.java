package com.ott.userauth.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenStore {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-expiry-days}")
    private long refreshExpiryDays;

    // Issue a new opaque refresh token for this user
    public String create(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + token,
                String.valueOf(userId),
                Duration.ofDays(refreshExpiryDays));
        return token;
    }

    // Read the token AND delete it in one step, so it can only be used once (rotation)
    public Optional<Long> consume(String token) {
        return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(REFRESH_PREFIX + token))
                .map(Long::valueOf);
    }

    // Used by logout
    public void delete(String token) {
        redisTemplate.delete(REFRESH_PREFIX + token);
    }

    // Block an access token until it would have expired anyway
    public void blacklist(String jti, long remainingSeconds) {
        if (remainingSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(remainingSeconds));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}