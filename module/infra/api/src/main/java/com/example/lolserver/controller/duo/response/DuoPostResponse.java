package com.example.lolserver.controller.duo.response;

import com.example.lolserver.domain.duo.application.model.DuoPostReadModel;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;

import java.time.LocalDateTime;
import java.util.List;

public record DuoPostResponse(
        Long id,
        String primaryLane,
        String desiredLane,
        boolean hasMicrophone,
        String tier,
        String rank,
        int leaguePoints,
        String memo,
        String status,
        boolean tierAvailable,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static DuoPostResponse from(DuoPostReadModel readModel) {
        return new DuoPostResponse(
                readModel.getId(),
                readModel.getPrimaryLane(),
                readModel.getDesiredLane(),
                readModel.isHasMicrophone(),
                readModel.getTier(),
                readModel.getRank(),
                readModel.getLeaguePoints(),
                readModel.getMemo(),
                readModel.getStatus(),
                readModel.isTierAvailable(),
                readModel.getMostChampions(),
                readModel.getRecentGameSummary(),
                readModel.getExpiresAt(),
                readModel.getCreatedAt()
        );
    }
}
