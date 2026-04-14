package com.example.lolserver.domain.duo.application.model;

import com.example.lolserver.domain.duo.domain.DuoPost;
import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.summoner.domain.Summoner;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DuoMatchResultReadModel {
    private final Long duoPostId;
    private final Long requestId;
    private final String partnerGameName;
    private final String partnerTagLine;
    private final String status;

    public static DuoMatchResultReadModel of(DuoPost duoPost, DuoRequest duoRequest) {
        return DuoMatchResultReadModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(null)
                .partnerTagLine(null)
                .status(duoRequest.getStatus().name())
                .build();
    }

    public static DuoMatchResultReadModel of(DuoPost duoPost, DuoRequest duoRequest,
            Summoner partnerSummoner) {
        String partnerGameName = null;
        String partnerTagLine = null;
        if (partnerSummoner != null) {
            partnerGameName = partnerSummoner.getGameName();
            partnerTagLine = partnerSummoner.getTagLine();
        }
        return DuoMatchResultReadModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(partnerGameName)
                .partnerTagLine(partnerTagLine)
                .status(duoRequest.getStatus().name())
                .build();
    }
}
