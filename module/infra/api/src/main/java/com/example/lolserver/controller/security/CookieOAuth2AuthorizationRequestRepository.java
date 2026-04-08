package com.example.lolserver.controller.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final int EXPIRE_SECONDS = 300;

    private final ConcurrentHashMap<String, AuthorizationRequestEntry> store =
            new ConcurrentHashMap<>();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(
            HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }
        AuthorizationRequestEntry entry = store.get(state);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        return entry.request();
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            return;
        }

        evictExpired();

        String state = authorizationRequest.getState();
        store.put(state, new AuthorizationRequestEntry(
                authorizationRequest, Instant.now().plusSeconds(EXPIRE_SECONDS)));

        log.debug("[OAuth2 State] 저장 - state: {}, redirectUri: {}",
                state, authorizationRequest.getRedirectUri());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }
        AuthorizationRequestEntry entry = store.remove(state);
        if (entry == null || entry.isExpired()) {
            log.debug("[OAuth2 State] 조회 실패 - state: {}, 만료: {}",
                    state, entry != null);
            return null;
        }
        log.debug("[OAuth2 State] 조회 성공 - state: {}", state);
        return entry.request();
    }

    private void evictExpired() {
        Iterator<Map.Entry<String, AuthorizationRequestEntry>> it =
                store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
    }

    private record AuthorizationRequestEntry(
            OAuth2AuthorizationRequest request,
            Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
