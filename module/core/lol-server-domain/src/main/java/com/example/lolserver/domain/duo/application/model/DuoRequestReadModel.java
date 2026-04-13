package com.example.lolserver.domain.duo.application.model;

import com.example.lolserver.domain.duo.domain.DuoRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DuoRequestReadModel {
    private final Long id;
    private final Long duoPostId;
    private final String primaryLane;
    private final String secondaryLane;
    private final boolean hasMicrophone;
    private final String tier;
    private final String rank;
    private final int leaguePoints;
    private final String memo;
    private final String status;
    private final LocalDateTime createdAt;

    public static DuoRequestReadModel of(DuoRequest request) {
        return DuoRequestReadModel.builder()
                .id(request.getId())
                .duoPostId(request.getDuoPostId())
                .primaryLane(request.getPrimaryLane().name())
                .secondaryLane(request.getSecondaryLane().name())
                .hasMicrophone(request.isHasMicrophone())
                .tier(request.getTier())
                .rank(request.getRank())
                .leaguePoints(request.getLeaguePoints())
                .memo(request.getMemo())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
