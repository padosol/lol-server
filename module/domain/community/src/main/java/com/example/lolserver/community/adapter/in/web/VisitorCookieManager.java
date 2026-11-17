package com.example.lolserver.community.adapter.in.web;

import com.example.lolserver.common.config.CookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 비로그인 방문자를 식별하는 {@code visitorId} 쿠키를 읽고, 없거나 형식이 잘못되면 서버가 새로 발급한다.
 */
@Component
@RequiredArgsConstructor
public class VisitorCookieManager {

    static final String COOKIE_NAME = "visitorId";
    private static final String COOKIE_PATH = "/api";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    private final CookieProperties cookieProperties;

    public UUID resolveOrIssue(HttpServletRequest request, HttpServletResponse response) {
        UUID existing = readValidCookie(request);
        if (existing != null) {
            return existing;
        }
        UUID issued = UUID.randomUUID();
        addCookie(response, issued);
        return issued;
    }

    private UUID readValidCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return parseCanonical(cookie.getValue());
            }
        }
        return null;
    }

    private UUID parseCanonical(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            UUID parsed = UUID.fromString(raw);
            return parsed.toString().equals(raw) ? parsed : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void addCookie(HttpServletResponse response, UUID visitorId) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, visitorId.toString())
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(COOKIE_PATH)
                .maxAge(COOKIE_MAX_AGE);

        String domain = cookieProperties.getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
