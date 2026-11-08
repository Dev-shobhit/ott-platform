package com.ott.userauth.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Component
public class OtpStore {

    private static final String CODE_PREFIX = "otp:code:";
    private static final String ATTEMPTS_PREFIX = "otp:attempts:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";
    private static final String REQUESTS_PREFIX = "otp:requests:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${otp.expiry-minutes}")
    private long expiryMinutes;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    @Value("${otp.cooldown-seconds}")
    private long cooldownSeconds;

    @Value("${otp.max-requests}")
    private int maxRequests;

    @Value("${otp.request-window-minutes}")
    private long requestWindowMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    // True if the user asked for a code less than cooldown-seconds ago
    public boolean isOnCooldown(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(COOLDOWN_PREFIX + email));
    }

    // True if the user already asked for max-requests codes inside the window
    public boolean isRequestLimitReached(String email) {
        String count = redisTemplate.opsForValue().get(REQUESTS_PREFIX + email);
        return count != null && Long.parseLong(count) >= maxRequests;
    }

    // Create a new code, store only its hash, and return the plain code so it can be emailed
    public String generateAndStore(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        redisTemplate.opsForValue().set(CODE_PREFIX + email, hash(email, otp), Duration.ofMinutes(expiryMinutes));
        redisTemplate.delete(ATTEMPTS_PREFIX + email);
        redisTemplate.opsForValue().set(COOLDOWN_PREFIX + email, "1", Duration.ofSeconds(cooldownSeconds));

        Long requests = redisTemplate.opsForValue().increment(REQUESTS_PREFIX + email);
        if (requests != null && requests == 1) {
            redisTemplate.expire(REQUESTS_PREFIX + email, Duration.ofMinutes(requestWindowMinutes));
        }
        return otp;
    }

    // True only if the code matches. A used code is deleted; too many wrong tries also deletes it.
    public boolean verify(String email, String otp) {
        String stored = redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (stored == null) {
            return false;
        }

        Long attempts = redisTemplate.opsForValue().increment(ATTEMPTS_PREFIX + email);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(ATTEMPTS_PREFIX + email, Duration.ofMinutes(expiryMinutes));
        }
        if (attempts != null && attempts > maxAttempts) {
            clear(email);
            return false;
        }

        boolean match = MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                hash(email, otp).getBytes(StandardCharsets.UTF_8));
        if (match) {
            clear(email);
        }
        return match;
    }

    private void clear(String email) {
        redisTemplate.delete(CODE_PREFIX + email);
        redisTemplate.delete(ATTEMPTS_PREFIX + email);
    }

    private String hash(String email, String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((email + ":" + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}