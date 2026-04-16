package com.example.lolserver.domain.member.application.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class OAuthUserInfo {

    private String provider;
    private String providerId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String puuid;
}
