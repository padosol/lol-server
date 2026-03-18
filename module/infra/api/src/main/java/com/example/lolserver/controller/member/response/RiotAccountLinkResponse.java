package com.example.lolserver.controller.member.response;

import com.example.lolserver.domain.member.application.model.RiotAccountLinkReadModel;

import java.time.LocalDateTime;

public record RiotAccountLinkResponse(
        Long id,
        String puuid,
        String gameName,
        String tagLine,
        String platformId,
        LocalDateTime linkedAt
) {
    public static RiotAccountLinkResponse from(RiotAccountLinkReadModel readModel) {
        return new RiotAccountLinkResponse(
                readModel.getId(),
                readModel.getPuuid(),
                readModel.getGameName(),
                readModel.getTagLine(),
                readModel.getPlatformId(),
                readModel.getLinkedAt()
        );
    }
}
