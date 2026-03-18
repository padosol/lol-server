package com.example.lolserver.domain.member.domain;

import com.example.lolserver.domain.member.application.model.OAuthUserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiotAccountLink {

    private Long id;
    private Long memberId;
    private String puuid;
    private String gameName;
    private String tagLine;
    private String platformId;
    private LocalDateTime linkedAt;

    public static RiotAccountLink create(Long memberId, OAuthUserInfo riotInfo, String platformId) {
        return RiotAccountLink.builder()
                .memberId(memberId)
                .puuid(riotInfo.getPuuid())
                .gameName(riotInfo.getGameName())
                .tagLine(riotInfo.getTagLine())
                .platformId(platformId)
                .linkedAt(LocalDateTime.now())
                .build();
    }
}
