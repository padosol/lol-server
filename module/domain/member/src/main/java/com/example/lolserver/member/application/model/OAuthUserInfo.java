package com.example.lolserver.member.application.model;

import com.example.lolserver.member.domain.vo.RiotAccount;
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
    private String gameName;
    private String tagLine;

    /** RIOT 연동이면 puuid·Riot ID, 아니면 null. */
    public RiotAccount toRiotAccount() {
        return puuid == null ? null : new RiotAccount(puuid, gameName, tagLine);
    }
}
