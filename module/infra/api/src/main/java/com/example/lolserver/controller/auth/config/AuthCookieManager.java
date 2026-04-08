package com.example.lolserver.controller.auth.config;

import com.example.lolserver.controller.security.JwtProperties;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
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

    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    public void addAuthCookies(HttpServletResponse response, AuthTokenReadModel tokenReadModel) {
        addCookie(response, ACCESS_TOKEN_COOKIE, tokenReadModel.accessToken(),
                "/", (int) jwtProperties.getAccessTokenExpiry());
        addCookie(response, REFRESH_TOKEN_COOKIE, tokenReadModel.refreshToken(),
                "/api/auth/refresh", (int) jwtProperties.getRefreshTokenExpiry());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        deleteCookie(response, ACCESS_TOKEN_COOKIE, "/");
        deleteCookie(response, REFRESH_TOKEN_COOKIE, "/api/auth/refresh");
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
