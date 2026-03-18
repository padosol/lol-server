package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTokenExchanger {

    private final RestClient oauthRestClient;

    @SuppressWarnings("unchecked")
    public String exchange(String code, String redirectUri,
                           OAuthProperties.ProviderConfig config, String providerName) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        try {
            Map<String, Object> response = oauthRestClient.post()
                    .uri(config.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                        providerName + " 토큰 교환에 실패했습니다.");
            }

            return (String) response.get("access_token");
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 토큰 교환 실패: {}", providerName, e.getMessage());
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                    providerName + " 토큰 교환에 실패했습니다.");
        }
    }
}
