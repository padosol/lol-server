package com.example.lolserver.controller.duo.response;

import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;

import java.time.LocalDateTime;
import java.util.List;

public record DuoPostListResponse(
        Long id,
        String primaryLane,
        String desiredLane,
        boolean hasMicrophone,
        String tier,
        String rank,
        int leaguePoints,
        String memo,
        String status,
        int requestCount,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static DuoPostListResponse from(DuoPostListReadModel readModel) {
        return new DuoPostListResponse(
                readModel.getId(),
                readModel.getPrimaryLane(),
                readModel.getDesiredLane(),
                readModel.isHasMicrophone(),
                readModel.getTier(),
                readModel.getRank(),
                readModel.getLeaguePoints(),
                readModel.getMemo(),
                readModel.getStatus(),
                readModel.getRequestCount(),
                readModel.getMostChampions(),
                readModel.getRecentGameSummary(),
                readModel.getExpiresAt(),
                readModel.getCreatedAt()
        );
    }
}
