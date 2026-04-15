package com.example.lolserver.controller.duo.response;

import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;

import java.time.LocalDateTime;
import java.util.List;

public record DuoRequestResponse(
        Long id,
        Long duoPostId,
        String primaryLane,
        String desiredLane,
        boolean hasMicrophone,
        String tier,
        String rank,
        int leaguePoints,
        String memo,
        String status,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary,
        LocalDateTime createdAt
) {
    public static DuoRequestResponse from(DuoRequestReadModel readModel) {
        return new DuoRequestResponse(
                readModel.getId(),
                readModel.getDuoPostId(),
                readModel.getPrimaryLane(),
                readModel.getDesiredLane(),
                readModel.isHasMicrophone(),
                readModel.getTier(),
                readModel.getRank(),
                readModel.getLeaguePoints(),
                readModel.getMemo(),
                readModel.getStatus(),
                readModel.getMostChampions(),
                readModel.getRecentGameSummary(),
                readModel.getCreatedAt()
        );
    }
}
