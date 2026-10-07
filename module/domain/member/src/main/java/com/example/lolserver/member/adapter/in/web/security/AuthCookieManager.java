package com.example.lolserver.member.adapter.in.web.security;

import com.example.lolserver.member.config.CookieProperties;
import com.example.lolserver.member.config.JwtProperties;
import com.example.lolserver.member.application.model.resultmodel.AuthTokenResultModel;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthCookieManager {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    static final String REFRESH_TOKEN_PATH = "/api/v1/auth/refresh";
    /**
     * 옛 refresh 경로. 브라우저는 Path 가 일치하는 경로에만 쿠키를 보내므로, lol-ui 가 옛
     * {@code /api/auth/refresh} 를 호출하는 병행 기간에는 같은 refresh 토큰을 두 Path 에 모두 발급한다.
     * 새 Path 로만 발급하면 옛 경로 호출에 쿠키가 실리지 않아 사용자가 로그아웃된다.
     * lol-ui 전환 + refresh 토큰 유효기간(14일) 경과 후 MP-148 에서 제거.
     */
    static final String LEGACY_REFRESH_TOKEN_PATH = "/api/auth/refresh";

    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    public void addAuthCookies(HttpServletResponse response, AuthTokenResultModel tokenReadModel) {
        addCookie(response, ACCESS_TOKEN_COOKIE, tokenReadModel.accessToken(),
                "/", (int) jwtProperties.getAccessTokenExpiry());
        addCookie(response, REFRESH_TOKEN_COOKIE, tokenReadModel.refreshToken(),
                REFRESH_TOKEN_PATH, (int) jwtProperties.getRefreshTokenExpiry());
        addCookie(response, REFRESH_TOKEN_COOKIE, tokenReadModel.refreshToken(),
                LEGACY_REFRESH_TOKEN_PATH, (int) jwtProperties.getRefreshTokenExpiry());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        deleteCookie(response, ACCESS_TOKEN_COOKIE, "/");
        deleteCookie(response, REFRESH_TOKEN_COOKIE, REFRESH_TOKEN_PATH);
        deleteCookie(response, REFRESH_TOKEN_COOKIE, LEGACY_REFRESH_TOKEN_PATH);
    }

    public String extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, int maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(path)
                .maxAge(maxAge);

        String domain = cookieProperties.getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void deleteCookie(HttpServletResponse response, String name, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(path)
                .maxAge(0);

        String domain = cookieProperties.getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
