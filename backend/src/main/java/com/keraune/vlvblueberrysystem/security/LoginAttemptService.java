package com.keraune.vlvblueberrysystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Small in-memory rate limiter. Production deployments can replace it with a shared store. */
@Service
public class LoginAttemptService {
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowSeconds;

    public LoginAttemptService(
            @Value("${blueberrytrace.security.login.max-attempts:5}") int maxAttempts,
            @Value("${blueberrytrace.security.login.window-seconds:900}") long windowSeconds
    ) {
        this.maxAttempts = Math.max(3, maxAttempts);
        this.windowSeconds = Math.max(60, windowSeconds);
    }

    public void checkAllowed(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt != null && attempt.expiresAt().isAfter(Instant.now()) && attempt.count() >= maxAttempts) {
            throw new IllegalArgumentException("No se pudo iniciar sesión. Espera unos minutos e inténtalo nuevamente.");
        }
    }

    public void failed(String key) {
        attempts.compute(key, (ignored, current) -> {
            Instant now = Instant.now();
            if (current == null || current.expiresAt().isBefore(now)) {
                return new Attempt(1, now.plusSeconds(windowSeconds));
            }
            return new Attempt(current.count() + 1, current.expiresAt());
        });
    }

    public void succeeded(String key) {
        attempts.remove(key);
    }

    private record Attempt(int count, Instant expiresAt) {}
}
