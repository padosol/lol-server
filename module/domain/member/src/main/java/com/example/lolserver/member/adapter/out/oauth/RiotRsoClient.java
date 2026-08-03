package com.example.lolserver.member.adapter.out.oauth;

import com.example.lolserver.member.config.OAuthProperties;
import com.example.lolserver.member.application.port.out.RiotAccountPort;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiotRsoClient implements RiotAccountPort {

    private final RestClient oauthRestClient;
    private final OAuthProperties oAuthProperties;

    @SuppressWarnings("unchecked")
    @Override
    public String fetchPuuid(String accessToken) {
        OAuthProperties.ProviderConfig config =
                oAuthProperties.getProviderConfig("riot");
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

            return puuid;
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Riot PUUID 조회 실패: {}", e.getMessage());
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                    "Riot PUUID 조회에 실패했습니다.");
        }
    }
}
