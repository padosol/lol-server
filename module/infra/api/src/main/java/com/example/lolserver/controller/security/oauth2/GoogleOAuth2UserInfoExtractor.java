package com.example.lolserver.controller.security.oauth2;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleOAuth2UserInfoExtractor
        implements OAuth2UserInfoExtractor {

    @Override
    public String getRegistrationId() {
        return "google";
    }

    @Override
    public OAuthUserInfo extract(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        return OAuthUserInfo.builder()
                .provider("GOOGLE")
                .providerId((String) attributes.get("id"))
                .email((String) attributes.get("email"))
                .nickname((String) attributes.get("name"))
                .profileImageUrl((String) attributes.get("picture"))
                .build();
    }
}
