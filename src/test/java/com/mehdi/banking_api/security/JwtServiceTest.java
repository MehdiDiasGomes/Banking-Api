package com.mehdi.banking_api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "xww1xlHu6rt4YSc6oHhmH8tGY/wccOVx47GejuxaSKs=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET_KEY", SECRET);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken("user@test.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String email = "user@test.com";
        String token = jwtService.generateToken(email);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        String token = jwtService.generateToken("user@test.com");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_withTamperedToken_returnsFalse() {
        String token = jwtService.generateToken("user@test.com");
        assertThat(jwtService.isTokenValid(token + "tampered")).isFalse();
    }

    @Test
    void isTokenValid_withRandomString_returnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }
}
