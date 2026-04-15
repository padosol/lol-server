package com.example.lolserver.domain.duo.application.model;

import com.example.lolserver.domain.duo.domain.DuoRequest;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DuoRequestReadModel {
    private final Long id;
    private final Long duoPostId;
    private final String primaryLane;
    private final String desiredLane;
    private final boolean hasMicrophone;
    private final String tier;
    private final String rank;
    private final int leaguePoints;
    private final String memo;
    private final String status;
    private final List<MostChampion> mostChampions;
    private final RecentGameSummary recentGameSummary;
    private final LocalDateTime createdAt;

    public static DuoRequestReadModel of(DuoRequest request) {
        return DuoRequestReadModel.builder()
                .id(request.getId())
                .duoPostId(request.getDuoPostId())
                .primaryLane(request.getPrimaryLane().name())
                .desiredLane(request.getDesiredLane().name())
                .hasMicrophone(request.isHasMicrophone())
                .tier(request.getTier())
                .rank(request.getRank())
                .leaguePoints(request.getLeaguePoints())
                .memo(request.getMemo())
                .status(request.getStatus().name())
                .mostChampions(request.getMostChampions())
                .recentGameSummary(request.getRecentGameSummary())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
