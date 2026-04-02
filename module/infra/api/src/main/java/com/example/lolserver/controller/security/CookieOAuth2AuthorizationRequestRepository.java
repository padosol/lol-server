package com.example.lolserver.controller.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 300;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(
            HttpServletRequest request) {
        return getCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response);
            return;
        }

        String serialized = serialize(authorizationRequest);
        if (serialized == null) {
            return;
        }

        Cookie cookie = new Cookie(COOKIE_NAME, serialized);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest =
                getCookie(request);
        deleteCookie(response);
        return authorizationRequest;
    }

    private OAuth2AuthorizationRequest getCookie(
            HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return deserialize(cookie.getValue());
            }
        }
        return null;
    }

    private void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @SuppressWarnings("unchecked")
    private String serialize(OAuth2AuthorizationRequest request) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("authorizationUri", request.getAuthorizationUri());
            data.put("clientId", request.getClientId());
            data.put("redirectUri", request.getRedirectUri());
            data.put("scopes", request.getScopes());
            data.put("state", request.getState());
            data.put("authorizationRequestUri",
                    request.getAuthorizationRequestUri());
            data.put("attributes", request.getAttributes());
            if (request.getAdditionalParameters() != null) {
                data.put("additionalParameters",
                        request.getAdditionalParameters());
            }

            String json = objectMapper.writeValueAsString(data);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            Map<String, Object> data =
                    objectMapper.readValue(bytes, Map.class);

            Set<String> scopes = Set.copyOf(
                    (java.util.Collection<String>) data.get("scopes"));
            Map<String, Object> attributes =
                    data.get("attributes") != null
                            ? (Map<String, Object>) data.get("attributes")
                            : Map.of();
            Map<String, Object> additionalParameters =
                    data.get("additionalParameters") != null
                            ? (Map<String, Object>)
                                    data.get("additionalParameters")
                            : Map.of();

            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(
                            (String) data.get("authorizationUri"))
                    .clientId((String) data.get("clientId"))
                    .redirectUri((String) data.get("redirectUri"))
                    .scopes(scopes)
                    .state((String) data.get("state"))
                    .authorizationRequestUri(
                            (String) data.get("authorizationRequestUri"))
                    .attributes(attrs -> attrs.putAll(attributes))
                    .additionalParameters(
                            params -> params.putAll(additionalParameters))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }
}
