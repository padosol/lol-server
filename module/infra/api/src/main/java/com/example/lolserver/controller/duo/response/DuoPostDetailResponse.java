package com.example.lolserver.controller.duo.response;

import com.example.lolserver.domain.duo.application.model.DuoPostDetailReadModel;
import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;

import java.time.LocalDateTime;
import java.util.List;

public record DuoPostDetailResponse(
        Long id,
        String primaryLane,
        String desiredLane,
        boolean hasMicrophone,
        String tier,
        String rank,
        int leaguePoints,
        String memo,
        String status,
        boolean isOwner,
        List<MostChampion> mostChampions,
        RecentGameSummary recentGameSummary,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        List<DuoRequestResponse> requests
) {
    public static DuoPostDetailResponse from(DuoPostDetailReadModel readModel) {
        List<DuoRequestResponse> requestResponses = readModel.getRequests() != null
                ? readModel.getRequests().stream()
                        .map(DuoRequestResponse::from)
                        .toList()
                : List.of();

        return new DuoPostDetailResponse(
                readModel.getId(),
                readModel.getPrimaryLane(),
                readModel.getDesiredLane(),
                readModel.isHasMicrophone(),
                readModel.getTier(),
                readModel.getRank(),
                readModel.getLeaguePoints(),
                readModel.getMemo(),
                readModel.getStatus(),
                readModel.isOwner(),
                readModel.getMostChampions(),
                readModel.getRecentGameSummary(),
                readModel.getExpiresAt(),
                readModel.getCreatedAt(),
                requestResponses
        );
    }
}
