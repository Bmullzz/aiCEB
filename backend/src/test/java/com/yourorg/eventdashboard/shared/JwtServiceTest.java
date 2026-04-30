package com.yourorg.eventdashboard.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-32-chars-minimum!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 8L);
    }

    @Test
    void generateToken_returnsNonEmptyString() {
        String token = jwtService.generateToken("admin");
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void validateToken_returnsCorrectUsername() {
        String token = jwtService.generateToken("admin");
        String subject = jwtService.validateToken(token);
        assertThat(subject).isEqualTo("admin");
    }

    @Test
    void validateToken_throwsForGarbageInput() {
        assertThatThrownBy(() -> jwtService.validateToken("not.a.jwt"))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessageContaining("INVALID_TOKEN");
    }

    @Test
    void validateToken_throwsForExpiredToken() {
        // expiryHours = -1 produces a token whose expiration is 1 hour in the past
        JwtService expiredService = new JwtService(SECRET, -1L);
        String expiredToken = expiredService.generateToken("admin");

        assertThatThrownBy(() -> jwtService.validateToken(expiredToken))
                .isInstanceOf(JwtAuthenticationException.class);
    }

    @Test
    void validateToken_throwsWithTokenExpiredMessage_forExpiredToken() {
        JwtService expiredService = new JwtService(SECRET, -1L);
        String expiredToken = expiredService.generateToken("admin");

        assertThatThrownBy(() -> jwtService.validateToken(expiredToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessageContaining("TOKEN_EXPIRED");
    }
}
