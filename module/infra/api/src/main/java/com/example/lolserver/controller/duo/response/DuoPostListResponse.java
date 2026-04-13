package com.example.lolserver.controller.duo.response;

import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;

import java.time.LocalDateTime;

public record DuoPostListResponse(
        Long id,
        String primaryLane,
        String secondaryLane,
        boolean hasMicrophone,
        String tier,
        String rank,
        int leaguePoints,
        String memo,
        String status,
        int requestCount,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static DuoPostListResponse from(DuoPostListReadModel readModel) {
        return new DuoPostListResponse(
                readModel.getId(),
                readModel.getPrimaryLane(),
                readModel.getSecondaryLane(),
                readModel.isHasMicrophone(),
                readModel.getTier(),
                readModel.getRank(),
                readModel.getLeaguePoints(),
                readModel.getMemo(),
                readModel.getStatus(),
                readModel.getRequestCount(),
                readModel.getExpiresAt(),
                readModel.getCreatedAt()
        );
    }
}
