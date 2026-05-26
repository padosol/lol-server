package com.example.lolserver.duo.adapter.in.web.response;

import com.example.lolserver.duo.application.model.DuoMatchResultReadModel;

public record DuoMatchResultResponse(
        Long duoPostId,
        Long requestId,
        String partnerGameName,
        String partnerTagLine,
        String status
) {
    public static DuoMatchResultResponse from(DuoMatchResultReadModel readModel) {
        return new DuoMatchResultResponse(
                readModel.getDuoPostId(),
                readModel.getRequestId(),
                readModel.getPartnerGameName(),
                readModel.getPartnerTagLine(),
                readModel.getStatus()
        );
    }
}
