package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.adapter.oauth.dto.OAuthTokenResponse;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTokenExchanger {

    private final RestClient oauthRestClient;

    @SuppressWarnings("unchecked")
    public OAuthTokenResponse exchange(String code, String redirectUri,
                                       OAuthProperties.ProviderConfig config, OAuthProvider provider) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        RestClient.RequestBodySpec requestSpec = oauthRestClient.post()
                .uri(config.getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);

        if (provider == OAuthProvider.RIOT) {
            String credentials = config.getClientId() + ":" + config.getClientSecret();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestSpec.header("Authorization", "Basic " + encoded);
        } else {
            body.add("client_id", config.getClientId());
            body.add("client_secret", config.getClientSecret());
        }

        try {
            Map<String, Object> response = requestSpec
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                        provider.name() + " 토큰 교환에 실패했습니다.");
            }

            return OAuthTokenResponse.from(response);
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 토큰 교환 실패: {}", provider.name(), e.getMessage());
            throw new CoreException(ErrorType.OAUTH_LOGIN_FAILED,
                    provider.name() + " 토큰 교환에 실패했습니다.");
        }
    }
}
