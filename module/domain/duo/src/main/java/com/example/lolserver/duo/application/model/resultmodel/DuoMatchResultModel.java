package com.example.lolserver.duo.application.model.resultmodel;

import com.example.lolserver.duo.domain.DuoPost;
import com.example.lolserver.duo.domain.DuoRequest;
import com.example.lolserver.summoner.application.model.readmodel.SummonerReadModel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DuoMatchResultModel {
    private final Long duoPostId;
    private final Long requestId;
    private final String partnerGameName;
    private final String partnerTagLine;
    private final String status;

    public static DuoMatchResultModel of(DuoPost duoPost, DuoRequest duoRequest) {
        return DuoMatchResultModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(null)
                .partnerTagLine(null)
                .status(duoRequest.getStatus().name())
                .build();
    }

    public static DuoMatchResultModel of(DuoPost duoPost, DuoRequest duoRequest,
            SummonerReadModel partnerSummoner) {
        String partnerGameName = null;
        String partnerTagLine = null;
        if (partnerSummoner != null) {
            partnerGameName = partnerSummoner.getGameName();
            partnerTagLine = partnerSummoner.getTagLine();
        }
        return DuoMatchResultModel.builder()
                .duoPostId(duoPost.getId())
                .requestId(duoRequest.getId())
                .partnerGameName(partnerGameName)
                .partnerTagLine(partnerTagLine)
                .status(duoRequest.getStatus().name())
                .build();
    }
}
