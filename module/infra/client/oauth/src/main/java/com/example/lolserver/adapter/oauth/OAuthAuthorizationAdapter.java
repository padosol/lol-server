package com.example.lolserver.adapter.oauth;

import com.example.lolserver.adapter.oauth.config.OAuthProperties;
import com.example.lolserver.domain.member.application.port.out.OAuthAuthorizationPort;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuthAuthorizationAdapter implements OAuthAuthorizationPort {

    private final OAuthProperties oAuthProperties;

    @Override
    public String buildAuthorizationUrl(OAuthProvider provider, String state) {
        OAuthProperties.ProviderConfig config = getConfig(provider);

        return UriComponentsBuilder.fromUriString(config.getAuthorizationUri())
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", config.getCallbackUri())
                .queryParam("response_type", "code")
                .queryParam("scope", config.getScope())
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .encode()
                .build()
                .toUriString();
    }

    @Override
    public String getCallbackUri(OAuthProvider provider) {
        return getConfig(provider).getCallbackUri();
    }

    private OAuthProperties.ProviderConfig getConfig(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> oAuthProperties.getGoogle();
            case RIOT -> oAuthProperties.getRiot();
        };
    }
}
