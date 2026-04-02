package com.example.lolserver.controller.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CustomOidcUserService extends OidcUserService {

    private final RestClient restClient;
    private final String riotAccountUri;

    public CustomOidcUserService(
            @Value("${riot.account-uri:}") String riotAccountUri) {
        this.restClient = RestClient.create();
        this.riotAccountUri = riotAccountUri;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration()
                .getRegistrationId();
        log.debug("[OIDC] loadUser 시작 - provider: {}, accessToken 존재: {}",
                registrationId,
                userRequest.getAccessToken() != null);
        log.debug("[OIDC] tokenUri: {}, userInfoUri: {}",
                userRequest.getClientRegistration().getProviderDetails()
                        .getTokenUri(),
                userRequest.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUri());

        OidcUser oidcUser;
        try {
            oidcUser = super.loadUser(userRequest);
            log.debug("[OIDC] super.loadUser 성공 - sub: {}, claims: {}",
                    oidcUser.getSubject(),
                    oidcUser.getClaims() != null
                            ? oidcUser.getClaims().keySet() : "null");
        } catch (OAuth2AuthenticationException e) {
            log.error("[OIDC] OAuth2AuthenticationException: errorCode={}, message={}",
                    e.getError().getErrorCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("OIDC 사용자 정보 로드 실패: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("oidc_user_load_error"),
                    "OIDC 사용자 정보 로드에 실패했습니다.", e);
        }

        if (!"riot".equals(registrationId)) {
            return oidcUser;
        }

        return enrichWithRiotAccountInfo(userRequest, oidcUser);
    }

    @SuppressWarnings("unchecked")
    private OidcUser enrichWithRiotAccountInfo(
            OidcUserRequest userRequest, OidcUser oidcUser) {
        String accessToken = userRequest.getAccessToken().getTokenValue();

        try {
            Map<String, Object> accountInfo = restClient.get()
                    .uri(riotAccountUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (accountInfo == null) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("riot_account_error"),
                        "Riot 계정 정보 조회에 실패했습니다.");
            }

            Map<String, Object> mergedAttributes =
                    new HashMap<>(oidcUser.getAttributes());
            mergedAttributes.put("puuid", accountInfo.get("puuid"));
            mergedAttributes.put("gameName", accountInfo.get("gameName"));
            mergedAttributes.put("tagLine", accountInfo.get("tagLine"));

            return new DefaultOidcUser(
                    oidcUser.getAuthorities(),
                    oidcUser.getIdToken(),
                    new OidcUserInfo(mergedAttributes),
                    "sub");
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Riot Account API 호출 실패: {}", e.getMessage());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("riot_account_error"),
                    "Riot 계정 정보 조회에 실패했습니다.");
        }
    }
}
