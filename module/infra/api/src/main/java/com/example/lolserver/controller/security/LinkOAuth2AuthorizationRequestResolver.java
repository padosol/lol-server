package com.example.lolserver.controller.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Slf4j
public class LinkOAuth2AuthorizationRequestResolver
        implements OAuth2AuthorizationRequestResolver {

    private static final String LINK_TOKEN_PARAM = "link_token";
    private static final String LINK_MEMBER_ID_ATTR = "link_member_id";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;
    private final SocialAccountLinkTokenStore linkTokenStore;

    public LinkOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            String authorizationRequestBaseUri,
            SocialAccountLinkTokenStore linkTokenStore) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, authorizationRequestBaseUri);
        this.linkTokenStore = linkTokenStore;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest =
                defaultResolver.resolve(request);
        return customizeIfLinkRequest(request, authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest =
                defaultResolver.resolve(request, clientRegistrationId);
        return customizeIfLinkRequest(request, authRequest);
    }

    private OAuth2AuthorizationRequest customizeIfLinkRequest(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null) {
            return null;
        }

        String linkToken = request.getParameter(LINK_TOKEN_PARAM);
        if (linkToken == null) {
            return authRequest;
        }

        Long memberId = linkTokenStore.consumeToken(linkToken);
        if (memberId == null) {
            log.warn("유효하지 않거나 만료된 link token: {}", linkToken);
            return authRequest;
        }

        log.debug("소셜 계정 연동 요청 - memberId: {}", memberId);

        return OAuth2AuthorizationRequest.from(authRequest)
                .attributes(attrs -> attrs.put(LINK_MEMBER_ID_ATTR, memberId))
                .build();
    }
}
