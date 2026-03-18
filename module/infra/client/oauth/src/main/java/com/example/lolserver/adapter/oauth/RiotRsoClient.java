package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
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
public class RiotRsoClient {

    private final RestClient oauthRestClient;
    private final OAuthProperties oAuthProperties;
    private final OAuthTokenExchanger tokenExchanger;

    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        OAuthProperties.ProviderConfig config = oAuthProperties.getRiot();

        String accessToken = tokenExchanger.exchange(code, redirectUri, config, "Riot");
        return fetchAccountInfo(accessToken, config);
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo fetchAccountInfo(String accessToken,
                                           OAuthProperties.ProviderConfig config) {
        try {
            Map<String, Object> response = oauthRestClient.get()
                    .uri(config.getAccountUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

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
                    .puuid(puuid)
                    .gameName((String) response.get("gameName"))
                    .tagLine((String) response.get("tagLine"))
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
