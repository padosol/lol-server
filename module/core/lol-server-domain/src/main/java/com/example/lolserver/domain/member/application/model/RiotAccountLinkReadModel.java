package com.example.lolserver.domain.member.application.model;

import com.example.lolserver.domain.member.domain.RiotAccountLink;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RiotAccountLinkReadModel {

    private Long id;
    private String puuid;
    private String gameName;
    private String tagLine;
    private String platformId;
    private LocalDateTime linkedAt;

    public static RiotAccountLinkReadModel of(RiotAccountLink link) {
        return RiotAccountLinkReadModel.builder()
                .id(link.getId())
                .puuid(link.getPuuid())
                .gameName(link.getGameName())
                .tagLine(link.getTagLine())
                .platformId(link.getPlatformId())
                .linkedAt(link.getLinkedAt())
                .build();
    }
}
