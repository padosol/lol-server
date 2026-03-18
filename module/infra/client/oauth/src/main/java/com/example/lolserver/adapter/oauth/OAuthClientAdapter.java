package com.example.lolserver.adapter.oauth;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.out.OAuthClientPort;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthClientAdapter implements OAuthClientPort {

    private final GoogleOAuthClient googleOAuthClient;
    private final RiotRsoClient riotRsoClient;

    @Override
    public OAuthUserInfo getUserInfo(OAuthProvider provider, String code, String redirectUri) {
        return switch (provider) {
            case GOOGLE -> googleOAuthClient.getUserInfo(code, redirectUri);
            case RIOT -> riotRsoClient.getUserInfo(code, redirectUri);
        };
    }
}
