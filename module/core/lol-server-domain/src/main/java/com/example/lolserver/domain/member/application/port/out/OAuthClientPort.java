package com.example.lolserver.domain.member.application.port.out;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;

public interface OAuthClientPort {

    OAuthUserInfo getUserInfo(OAuthProvider provider, String code, String redirectUri);
}
