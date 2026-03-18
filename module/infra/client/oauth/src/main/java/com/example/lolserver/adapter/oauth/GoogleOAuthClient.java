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
public class GoogleOAuthClient {

    private final RestClient oauthRestClient;
    private final OAuthProperties oAuthProperties;
    private final OAuthTokenExchanger tokenExchanger;

    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        OAuthProperties.ProviderConfig config = oAuthProperties.getGoogle();

        String accessToken = tokenExchanger.exchange(code, redirectUri, config, "Google");
        return fetchUserInfo(accessToken, config);
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo fetchUserInfo(String accessToken,
                                        OAuthProperties.ProviderConfig config) {
        try {
            Map<String, Object> response = oauthRestClient.get()
                    .uri(config.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                        "Google 사용자 정보 조회에 실패했습니다.");
            }

            String providerId = (String) response.get("id");
            if (providerId == null) {
                throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                        "Google 사용자 ID를 가져올 수 없습니다.");
            }

            return OAuthUserInfo.builder()
                    .provider(OAuthProvider.GOOGLE.name())
                    .providerId(providerId)
                    .email((String) response.get("email"))
                    .nickname((String) response.get("name"))
                    .profileImageUrl((String) response.get("picture"))
                    .build();
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google 사용자 정보 조회 실패: {}", e.getMessage());
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                    "Google 사용자 정보 조회에 실패했습니다.");
        }
    }
}
