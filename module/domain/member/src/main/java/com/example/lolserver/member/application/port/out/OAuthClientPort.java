package com.example.lolserver.member.application.port.out;

import com.example.lolserver.member.application.model.OAuthUserInfo;
import com.example.lolserver.member.domain.vo.OAuthProvider;

public interface OAuthClientPort {

    OAuthUserInfo getUserInfo(OAuthProvider provider, String code, String redirectUri);
}
