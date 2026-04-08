package com.example.lolserver.controller.security.oauth2;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserInfoExtractor {

    String getRegistrationId();

    OAuthUserInfo extract(OAuth2User oauth2User);
}
