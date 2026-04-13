package com.example.lolserver.controller.security;

import com.example.lolserver.controller.auth.config.AuthCookieManager;
import com.example.lolserver.controller.auth.config.OAuthCallbackProperties;
import com.example.lolserver.controller.security.oauth2.OAuth2UserInfoExtractor;
import com.example.lolserver.domain.member.application.model.AuthTokenReadModel;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.in.MemberAuthUseCase;
import com.example.lolserver.support.error.CoreException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private static final String LINK_MEMBER_ID_ATTR = "link_member_id";

    private final MemberAuthUseCase memberAuthUseCase;
    private final OAuthCallbackProperties oAuthCallbackProperties;
    private final CookieOAuth2AuthorizationRequestRepository
            authorizationRequestRepository;
    private final Map<String, OAuth2UserInfoExtractor> extractors;
    private final AuthCookieManager authCookieManager;

    public OAuth2AuthenticationSuccessHandler(
            MemberAuthUseCase memberAuthUseCase,
            OAuthCallbackProperties oAuthCallbackProperties,
            CookieOAuth2AuthorizationRequestRepository
                    authorizationRequestRepository,
            List<OAuth2UserInfoExtractor> extractorList,
            AuthCookieManager authCookieManager) {
        this.memberAuthUseCase = memberAuthUseCase;
        this.oAuthCallbackProperties = oAuthCallbackProperties;
        this.authorizationRequestRepository =
                authorizationRequestRepository;
        this.extractors = extractorList.stream()
                .collect(Collectors.toMap(
                        OAuth2UserInfoExtractor::getRegistrationId,
                        Function.identity()));
        this.authCookieManager = authCookieManager;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        try {
            OAuth2AuthenticationToken oauthToken =
                    (OAuth2AuthenticationToken) authentication;
            String registrationId =
                    oauthToken.getAuthorizedClientRegistrationId();
            OAuth2User oauth2User = oauthToken.getPrincipal();

            OAuth2UserInfoExtractor extractor =
                    extractors.get(registrationId);
            if (extractor == null) {
                throw new CoreException(
                        com.example.lolserver.support.error
                                .ErrorType.OAUTH_LOGIN_FAILED,
                        "지원하지 않는 OAuth 프로바이더: "
                                + registrationId);
            }

            OAuthUserInfo userInfo = extractor.extract(oauth2User);
            log.info("userInfo: {}", userInfo);

            Long linkMemberId = extractLinkMemberId(request);
            if (linkMemberId != null) {
                memberAuthUseCase.linkSocialAccount(
                        linkMemberId, userInfo);
                response.sendRedirect(buildLinkSuccessRedirectUrl());
            } else {
                AuthTokenReadModel result =
                        memberAuthUseCase.loginWithOAuthUserInfo(userInfo);
                authCookieManager.addAuthCookies(response, result);
                response.sendRedirect(
                        oAuthCallbackProperties.getFrontendCallbackUrl());
            }
        } catch (CoreException e) {
            log.error("OAuth2 처리 중 오류: {}", e.getMessage());
            response.sendRedirect(
                    buildErrorRedirectUrl(e.getErrorType().name()));
        }
    }

    private Long extractLinkMemberId(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }
        OAuth2AuthorizationRequest authRequest =
                authorizationRequestRepository
                        .loadAuthorizationRequest(request);
        if (authRequest == null) {
            return null;
        }
        Map<String, Object> attrs =
                authRequest.getAttributes();
        Object memberId = attrs.get(LINK_MEMBER_ID_ATTR);
        if (memberId == null) {
            return null;
        }
        return Long.valueOf(memberId.toString());
    }

    private String buildLinkSuccessRedirectUrl() {
        return UriComponentsBuilder
                .fromUriString(
                        oAuthCallbackProperties.getFrontendCallbackUrl())
                .fragment("linkSuccess=true")
                .build().toUriString();
    }

    private String buildErrorRedirectUrl(String error) {
        return UriComponentsBuilder
                .fromUriString(
                        oAuthCallbackProperties.getFrontendCallbackUrl())
                .fragment("error=" + error)
                .build().toUriString();
    }
}
