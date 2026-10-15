package com.applycoding.ledger.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.applycoding.ledger.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-secret-key-32-bytes-min";

    @Test
    void roundTripsTheUserId() {
        JwtService jwtService = service(Duration.ofHours(1));
        UUID userId = UUID.randomUUID();

        String token = jwtService.generate(userId, "ada@example.com");

        assertEquals(userId, jwtService.parseUserId(token));
        assertEquals(3600, jwtService.expiresInSeconds());
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        JwtService jwtService = service(Duration.ofHours(1));
        String token = jwtService.generate(UUID.randomUUID(), "ada@example.com");
        String tampered = token.substring(0, token.length() - 2) + "aa";

        assertThrows(JwtException.class, () -> jwtService.parseUserId(tampered));
        assertThrows(JwtException.class, () -> service(Duration.ofSeconds(-30)).parseUserId(
                service(Duration.ofSeconds(-30)).generate(UUID.randomUUID(), "ada@example.com")));
    }

    @Test
    void rejectsAShortSecret() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(new JwtProperties("too-short", Duration.ofMinutes(5)), Clock.systemUTC()));
    }

    private static JwtService service(Duration expiration) {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC);
        return new JwtService(new JwtProperties(SECRET, expiration), clock);
    }
}
