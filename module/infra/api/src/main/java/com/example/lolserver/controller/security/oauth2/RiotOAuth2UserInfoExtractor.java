package com.example.lolserver.controller.security.oauth2;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import com.example.lolserver.domain.member.domain.vo.OAuthProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RiotOAuth2UserInfoExtractor
        implements OAuth2UserInfoExtractor {

    @Override
    public String getRegistrationId() {
        return "riot";
    }

    @Override
    public boolean isLoginAllowed() {
        return false;
    }

    @Override
    public OAuthUserInfo extract(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        return OAuthUserInfo.builder()
                .provider(OAuthProvider.RIOT.name())
                .providerId((String) attributes.get("sub"))
                .puuid((String) attributes.get("puuid"))
                .build();
    }
}
