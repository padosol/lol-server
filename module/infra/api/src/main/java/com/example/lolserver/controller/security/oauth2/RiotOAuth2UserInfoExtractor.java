package com.example.lolserver.controller.security.oauth2;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
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
    public OAuthUserInfo extract(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        String gameName = (String) attributes.get("gameName");
        String tagLine = (String) attributes.get("tagLine");
        String nickname = buildNickname(gameName, tagLine);

        return OAuthUserInfo.builder()
                .provider("RIOT")
                .providerId((String) attributes.get("sub"))
                .puuid((String) attributes.get("puuid"))
                .gameName(gameName)
                .tagLine(tagLine)
                .nickname(nickname)
                .build();
    }

    private String buildNickname(String gameName, String tagLine) {
        if (gameName == null) {
            return "Riot User";
        }
        if (tagLine == null) {
            return gameName;
        }
        return gameName + "#" + tagLine;
    }
}
