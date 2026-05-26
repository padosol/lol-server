package com.example.lolserver.member.application.port.out;

import com.example.lolserver.member.domain.vo.OAuthProvider;

public interface OAuthAuthorizationPort {

    String buildAuthorizationUrl(OAuthProvider provider, String state);
}
