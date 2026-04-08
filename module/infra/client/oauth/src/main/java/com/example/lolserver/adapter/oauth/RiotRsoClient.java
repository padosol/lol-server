package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.adapter.oauth.dto.OAuthTokenResponse;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.out.OAuthProviderClient;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiotRsoClient implements OAuthProviderClient {

    private final RestClient oauthRestClient;
    private final OAuthProperties oAuthProperties;
    private final OAuthTokenExchanger tokenExchanger;

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.RIOT;
    }

    @Override
    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        OAuthProperties.ProviderConfig config = oAuthProperties.getProviderConfig("riot");

        OAuthTokenResponse tokenResponse = tokenExchanger.exchange(
                code, redirectUri, config, OAuthProvider.RIOT);
        String accessToken = tokenResponse.getAccessToken();

        return fetchAccountInfo(accessToken, config);
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo fetchAccountInfo(
            String accessToken,
            OAuthProperties.ProviderConfig config) {
        try {
            log.info("access token: {}", accessToken);
            Map<String, Object> response = oauthRestClient.get()
                    .uri(config.getAccountUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            log.info("response: {}", response);

            if (response == null) {
                throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                        "Riot 계정 정보 조회에 실패했습니다.");
            }

            String puuid = (String) response.get("puuid");
            if (puuid == null) {
                throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                        "Riot PUUID를 가져올 수 없습니다.");
            }

            return OAuthUserInfo.builder()
                    .provider(OAuthProvider.RIOT.name())
                    .providerId(puuid)
                    .build();
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Riot 계정 정보 조회 실패: {}", e.getMessage());
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                    "Riot 계정 정보 조회에 실패했습니다.");
        }
    }
}
