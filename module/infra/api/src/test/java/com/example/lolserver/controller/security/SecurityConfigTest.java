package com.example.lolserver.controller.security;

import com.example.lolserver.controller.security.oauth2.CustomOidcUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Mock
    private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Mock
    private CookieOAuth2AuthorizationRequestRepository
            cookieAuthorizationRequestRepository;

    @Mock
    private CustomOidcUserService customOidcUserService;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(
                "test-secret-key-must-be-at-least-256-bits"
                        + "-long-for-hs256-algorithm");
        jwtProperties.setAccessTokenExpiry(1800);
        jwtProperties.setRefreshTokenExpiry(1209600);

        JwtTokenAdapter jwtTokenAdapter =
                new JwtTokenAdapter(jwtProperties);
        jwtTokenAdapter.init();

        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins(List.of(
                "http://localhost:3000", "http://localhost:8080",
                "http://lol-ui:3000", "https://metapick.me"));

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtTokenAdapter);

        securityConfig = new SecurityConfig(
                filter, corsProperties,
                oAuth2SuccessHandler,
                oAuth2FailureHandler,
                cookieAuthorizationRequestRepository,
                customOidcUserService);
    }

    @DisplayName("CORS 설정 소스가 정상적으로 생성된다")
    @Test
    void corsConfigurationSource() {
        var source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader("Origin", "http://localhost:3000");

        var config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins())
                .contains("http://localhost:3000", "https://metapick.me");
        assertThat(config.getAllowedMethods())
                .contains("GET", "POST", "PUT", "PATCH",
                        "DELETE", "OPTIONS");
        assertThat(config.getAllowCredentials()).isTrue();
    }
}
