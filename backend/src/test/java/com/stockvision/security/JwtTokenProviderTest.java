package com.stockvision.security;

import com.stockvision.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A";
    private JwtProperties jwtProperties;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpirationMs(3600000L); // 1 hour

        tokenProvider = new JwtTokenProvider(jwtProperties);
        tokenProvider.init();
    }

    @Test
    @DisplayName("Should generate and validate valid token with expected claims")
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "testuser", Role.ROLE_USER);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("testuser");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Should reject invalid signature or tampered token")
    void testRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "testuser", Role.ROLE_USER);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(tokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectExpiredToken() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(TEST_SECRET);
        expiredProps.setExpirationMs(-1000L); // expired 1 sec ago

        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        expiredProvider.init();

        String expiredToken = expiredProvider.generateToken(UUID.randomUUID(), "expired_user", Role.ROLE_USER);
        assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject null or malformed token")
    void testRejectMalformedToken() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
        assertThat(tokenProvider.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("Should fail fast on startup if secret is missing or too short")
    void testFailFastOnMissingOrWeakSecret() {
        JwtProperties emptyProps = new JwtProperties();
        emptyProps.setSecret("");
        JwtTokenProvider providerEmpty = new JwtTokenProvider(emptyProps);
        assertThatThrownBy(providerEmpty::init)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET must be configured");

        JwtProperties weakProps = new JwtProperties();
        weakProps.setSecret("too-short-secret");
        JwtTokenProvider providerWeak = new JwtTokenProvider(weakProps);
        assertThatThrownBy(providerWeak::init)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT secret is too weak");
    }
}