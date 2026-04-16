package com.example.lolserver.controller.security.oauth2;

import com.example.lolserver.domain.member.application.port.out.RiotAccountPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final RiotAccountPort riotAccountPort;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
            throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration()
                .getRegistrationId();
        log.info("[OIDC] loadUser 시작 - provider: {}", registrationId);

        try {
            OidcUser oidcUser = super.loadUser(userRequest);
            log.info("[OIDC] loadUser 성공 - sub: {}",
                    oidcUser.getSubject());

            if ("riot".equals(registrationId)) {
                String accessToken = userRequest.getAccessToken()
                        .getTokenValue();
                String puuid = riotAccountPort.fetchPuuid(accessToken);
                log.debug("[OIDC] Riot PUUID 조회 성공: {}", puuid);

                Map<String, Object> enrichedClaims =
                        new HashMap<>(oidcUser.getAttributes());
                enrichedClaims.put("puuid", puuid);

                return new DefaultOidcUser(
                        oidcUser.getAuthorities(),
                        oidcUser.getIdToken(),
                        new OidcUserInfo(enrichedClaims));
            }

            return oidcUser;
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
    }
}
