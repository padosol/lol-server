package com.example.lolserver.member.application.port.out;

import com.example.lolserver.member.application.model.OAuthUserInfo;
import com.example.lolserver.member.domain.vo.OAuthProvider;

public interface OAuthProviderClient {

    OAuthProvider getProvider();

    OAuthUserInfo getUserInfo(String code, String redirectUri);
}
