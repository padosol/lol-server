package com.example.lolserver.controller.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
        jwtProperties.setAccessTokenExpiry(1800);
        jwtProperties.setRefreshTokenExpiry(1209600);

        JwtTokenAdapter jwtTokenAdapter = new JwtTokenAdapter(jwtProperties);
        jwtTokenAdapter.init();

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenAdapter);
        securityConfig = new SecurityConfig(filter);
    }

    @DisplayName("CORS 설정 소스가 정상적으로 생성된다")
    @Test
    void corsConfigurationSource() {
        var source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader("Origin", "http://localhost:3000");

        var config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins())
                .contains("http://localhost:3000", "https://metapick.me");
        assertThat(config.getAllowedMethods())
                .contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowCredentials()).isTrue();
    }
}
