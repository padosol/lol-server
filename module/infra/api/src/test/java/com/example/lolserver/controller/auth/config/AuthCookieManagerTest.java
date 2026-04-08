package com.example.lolserver.controller.auth.config;

import com.example.lolserver.controller.security.JwtProperties;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthCookieManager 테스트")
class AuthCookieManagerTest {

    private AuthCookieManager authCookieManager;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("test-secret-key");
        jwtProperties.setAccessTokenExpiry(1800L);
        jwtProperties.setRefreshTokenExpiry(604800L);

        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setSecure(true);
        cookieProperties.setSameSite("Lax");
        cookieProperties.setDomain("");

        authCookieManager = new AuthCookieManager(jwtProperties, cookieProperties);
    }

    @DisplayName("addAuthCookies - accessToken, refreshToken 쿠키가 올바른 속성으로 설정된다")
    @Test
    void addAuthCookies() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthTokenReadModel tokenReadModel = new AuthTokenReadModel("access-token-value", "refresh-token-value", 1800);

        // when
        authCookieManager.addAuthCookies(response, tokenReadModel);

        // then
        List<String> setCookieHeaders = response.getHeaders("Set-Cookie");
        assertThat(setCookieHeaders).hasSize(2);

        String accessCookie = setCookieHeaders.stream()
                .filter(c -> c.startsWith("accessToken="))
                .findFirst()
                .orElse(null);
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie).contains("accessToken=access-token-value");
        assertThat(accessCookie).contains("HttpOnly");
        assertThat(accessCookie).contains("Secure");
        assertThat(accessCookie).contains("Path=/");
        assertThat(accessCookie).contains("Max-Age=1800");
        assertThat(accessCookie).contains("SameSite=Lax");

        String refreshCookie = setCookieHeaders.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .findFirst()
                .orElse(null);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie).contains("refreshToken=refresh-token-value");
        assertThat(refreshCookie).contains("HttpOnly");
        assertThat(refreshCookie).contains("Secure");
        assertThat(refreshCookie).contains("Path=/api/auth/refresh");
        assertThat(refreshCookie).contains("Max-Age=604800");
        assertThat(refreshCookie).contains("SameSite=Lax");
    }

    @DisplayName("clearAuthCookies - Max-Age=0으로 삭제 쿠키가 설정된다")
    @Test
    void clearAuthCookies() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        authCookieManager.clearAuthCookies(response);

        // then
        List<String> setCookieHeaders = response.getHeaders("Set-Cookie");
        assertThat(setCookieHeaders).hasSize(2);

        String accessCookie = setCookieHeaders.stream()
                .filter(c -> c.startsWith("accessToken="))
                .findFirst()
                .orElse(null);
        assertThat(accessCookie).isNotNull();
        assertThat(accessCookie).contains("Max-Age=0");
        assertThat(accessCookie).contains("Path=/");

        String refreshCookie = setCookieHeaders.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .findFirst()
                .orElse(null);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie).contains("Max-Age=0");
        assertThat(refreshCookie).contains("Path=/api/auth/refresh");
    }

    @DisplayName("extractAccessToken - 쿠키에서 accessToken을 올바르게 추출한다")
    @Test
    void extractAccessToken() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("accessToken", "my-access-token"),
                new Cookie("refreshToken", "my-refresh-token")
        );

        // when
        String result = authCookieManager.extractAccessToken(request);

        // then
        assertThat(result).isEqualTo("my-access-token");
    }

    @DisplayName("extractRefreshToken - 쿠키에서 refreshToken을 올바르게 추출한다")
    @Test
    void extractRefreshToken() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("accessToken", "my-access-token"),
                new Cookie("refreshToken", "my-refresh-token")
        );

        // when
        String result = authCookieManager.extractRefreshToken(request);

        // then
        assertThat(result).isEqualTo("my-refresh-token");
    }

    @DisplayName("extractAccessToken - 쿠키가 없을 때 null을 반환한다")
    @Test
    void extractAccessTokenWhenNoCookies() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();

        // when
        String result = authCookieManager.extractAccessToken(request);

        // then
        assertThat(result).isNull();
    }
}
