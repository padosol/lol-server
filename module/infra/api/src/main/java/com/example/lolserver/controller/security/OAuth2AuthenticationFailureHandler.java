package com.example.lolserver.controller.security;

import com.example.lolserver.controller.auth.config.OAuthCallbackProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    private final OAuthCallbackProperties oAuthCallbackProperties;
    private final CookieOAuth2AuthorizationRequestRepository
            authorizationRequestRepository;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        log.error("OAuth2 인증 실패: {}", exception.getMessage());

        String errorUrl = UriComponentsBuilder
                .fromUriString(
                        oAuthCallbackProperties.getFrontendCallbackUrl())
                .fragment("error=OAUTH_LOGIN_FAILED")
                .build().toUriString();

        authorizationRequestRepository.removeAuthorizationRequest(
                request, response);
        response.sendRedirect(errorUrl);
    }
}
