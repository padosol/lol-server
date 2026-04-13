package com.example.lolserver.controller.duo.response;

import com.example.lolserver.domain.duo.application.model.DuoPostReadModel;

import java.time.LocalDateTime;

public record DuoPostResponse(
        Long id,
        String primaryLane,
        String secondaryLane,
        boolean hasMicrophone,
        String tier,
        String rank,
        int leaguePoints,
        String memo,
        String status,
        boolean tierAvailable,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
    public static DuoPostResponse from(DuoPostReadModel readModel) {
        return new DuoPostResponse(
                readModel.getId(),
                readModel.getPrimaryLane(),
                readModel.getSecondaryLane(),
                readModel.isHasMicrophone(),
                readModel.getTier(),
                readModel.getRank(),
                readModel.getLeaguePoints(),
                readModel.getMemo(),
                readModel.getStatus(),
                readModel.isTierAvailable(),
                readModel.getExpiresAt(),
                readModel.getCreatedAt()
        );
    }
}
