package com.example.lolserver.controller.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenAdapterTest {

    private JwtTokenAdapter jwtTokenAdapter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecretKey("test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
        properties.setAccessTokenExpiry(1800);
        properties.setRefreshTokenExpiry(1209600);

        jwtTokenAdapter = new JwtTokenAdapter(properties);
        jwtTokenAdapter.init();
    }

    @DisplayName("Access Token을 생성하고 검증할 수 있다")
    @Test
    void generateAndValidateAccessToken() {
        // when
        String token = jwtTokenAdapter.generateAccessToken(1L, "USER");

        // then
        assertThat(jwtTokenAdapter.validateToken(token)).isTrue();
        assertThat(jwtTokenAdapter.getMemberIdFromToken(token)).isEqualTo(1L);
        assertThat(jwtTokenAdapter.getRoleFromToken(token)).isEqualTo("USER");
    }

    @DisplayName("Refresh Token을 생성하고 검증할 수 있다")
    @Test
    void generateAndValidateRefreshToken() {
        // when
        String token = jwtTokenAdapter.generateRefreshToken(2L, "ADMIN");

        // then
        assertThat(jwtTokenAdapter.validateToken(token)).isTrue();
        assertThat(jwtTokenAdapter.getMemberIdFromToken(token)).isEqualTo(2L);
        assertThat(jwtTokenAdapter.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @DisplayName("유효하지 않은 토큰은 검증에 실패한다")
    @Test
    void validateInvalidToken() {
        // when & then
        assertThat(jwtTokenAdapter.validateToken("invalid-token")).isFalse();
    }

    @DisplayName("Access Token 만료 시간을 반환한다")
    @Test
    void getAccessTokenExpiry() {
        // when & then
        assertThat(jwtTokenAdapter.getAccessTokenExpiry()).isEqualTo(1800L);
    }

    @DisplayName("Refresh Token 만료 시간을 반환한다")
    @Test
    void getRefreshTokenExpiry() {
        // when & then
        assertThat(jwtTokenAdapter.getRefreshTokenExpiry()).isEqualTo(1209600L);
    }
}
