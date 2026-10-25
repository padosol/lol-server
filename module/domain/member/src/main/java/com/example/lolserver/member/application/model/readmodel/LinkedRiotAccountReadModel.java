package com.example.lolserver.member.application.model.readmodel;

import com.example.lolserver.member.domain.SocialAccount;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원에 연동된 RIOT 계정. gameName·tagLine 은 연동 시점 Riot ID 이고, 그 전에 연동한 계정이면 null 이다.
 */
@Getter
@Builder
public class LinkedRiotAccountReadModel {

    private String puuid;
    private String gameName;
    private String tagLine;

    public static LinkedRiotAccountReadModel of(SocialAccount account) {
        return LinkedRiotAccountReadModel.builder()
                .puuid(account.getPuuid())
                .gameName(account.getGameName())
                .tagLine(account.getTagLine())
                .build();
    }
}
