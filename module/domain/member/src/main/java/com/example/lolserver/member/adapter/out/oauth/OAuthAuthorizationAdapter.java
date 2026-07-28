package com.example.lolserver.member.adapter.out.oauth;

import com.example.lolserver.member.config.OAuthProperties;
import com.example.lolserver.member.application.port.out.OAuthAuthorizationPort;
import com.example.lolserver.member.domain.vo.OAuthProvider;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuthAuthorizationAdapter implements OAuthAuthorizationPort {

    private final OAuthProperties oAuthProperties;

    @Override
    public String buildAuthorizationUrl(
            OAuthProvider provider, String state) {
        OAuthProperties.ProviderConfig config =
                getConfig(provider);

        return UriComponentsBuilder
                .fromUriString(config.getAuthorizationUri())
                .queryParam("client_id", config.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", config.getScope())
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    private OAuthProperties.ProviderConfig getConfig(
            OAuthProvider provider) {
        try {
            return oAuthProperties.getProviderConfig(
                    provider.name());
        } catch (IllegalArgumentException e) {
            throw new CoreException(
                    ErrorType.OAUTH_LOGIN_FAILED,
                    "지원하지 않는 OAuth 프로바이더: " + provider);
        }
    }
}
