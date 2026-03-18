package com.example.lolserver.domain.member.application.port.out;

import com.example.lolserver.domain.member.domain.vo.OAuthProvider;

public interface OAuthAuthorizationPort {

    String buildAuthorizationUrl(OAuthProvider provider, String state);

    String getCallbackUri(OAuthProvider provider);
}
