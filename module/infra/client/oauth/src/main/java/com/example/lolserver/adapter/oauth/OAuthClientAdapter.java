package com.example.lolserver.adapter.oauth;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.application.port.out.OAuthClientPort;
import com.example.lolserver.domain.member.application.port.out.OAuthProviderClient;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthClientAdapter implements OAuthClientPort {

    private final Map<OAuthProvider, OAuthProviderClient> clients;

    public OAuthClientAdapter(List<OAuthProviderClient> clientList) {
        this.clients = clientList.stream()
                .collect(Collectors.toMap(
                        OAuthProviderClient::getProvider,
                        Function.identity()));
    }

    @Override
    public OAuthUserInfo getUserInfo(
            OAuthProvider provider, String code, String redirectUri) {
        OAuthProviderClient client = clients.get(provider);
        if (client == null) {
            throw new CoreException(
                    ErrorType.OAUTH_LOGIN_FAILED,
                    "지원하지 않는 OAuth 프로바이더: " + provider);
        }
        return client.getUserInfo(code, redirectUri);
    }
}
