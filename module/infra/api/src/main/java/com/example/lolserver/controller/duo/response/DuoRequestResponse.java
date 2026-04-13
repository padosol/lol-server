package com.example.lolserver.controller.duo.response;

import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;

import java.time.LocalDateTime;

public record DuoRequestResponse(
        Long id,
        Long duoPostId,
        String primaryLane,
        String secondaryLane,
        boolean hasMicrophone,
        String tier,
        String rank,
        int leaguePoints,
        String memo,
        String status,
        LocalDateTime createdAt
) {
    public static DuoRequestResponse from(DuoRequestReadModel readModel) {
        return new DuoRequestResponse(
                readModel.getId(),
                readModel.getDuoPostId(),
                readModel.getPrimaryLane(),
                readModel.getSecondaryLane(),
                readModel.isHasMicrophone(),
                readModel.getTier(),
                readModel.getRank(),
                readModel.getLeaguePoints(),
                readModel.getMemo(),
                readModel.getStatus(),
                readModel.getCreatedAt()
        );
    }
}
