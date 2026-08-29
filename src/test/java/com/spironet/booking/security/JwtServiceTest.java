package com.spironet.booking.security;

import com.spironet.booking.entity.Role;
import com.spironet.booking.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-256-bits-long-value";

    private JwtService jwtService;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600000L);
        User user = User.builder()
                .id(1L)
                .username("jane")
                .email("jane@example.com")
                .password("irrelevant")
                .role(Role.USER)
                .enabled(true)
                .build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        String token = jwtService.generateToken(userPrincipal);

        assertNotNull(token);
        assertEquals("jane", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_forMatchingUser_returnsTrue() {
        String token = jwtService.generateToken(userPrincipal);

        assertTrue(jwtService.isTokenValid(token, userPrincipal));
    }

    @Test
    void isTokenValid_whenExpired_returnsFalse() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1L);
        String token = shortLivedJwtService.generateToken(userPrincipal);

        Thread.sleep(20);

        assertFalse(shortLivedJwtService.isTokenValid(token, userPrincipal));
    }

    @Test
    void isTokenValid_whenSignedWithDifferentSecret_returnsFalse() {
        JwtService otherJwtService = new JwtService("a-completely-different-secret-key-of-256-bits-minimum", 3600000L);
        String token = otherJwtService.generateToken(userPrincipal);

        assertFalse(jwtService.isTokenValid(token, userPrincipal));
    }
}
