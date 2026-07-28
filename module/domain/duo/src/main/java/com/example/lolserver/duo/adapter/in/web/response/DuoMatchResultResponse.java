package com.example.lolserver.duo.adapter.in.web.response;

import com.example.lolserver.duo.application.model.resultmodel.DuoMatchResultModel;

public record DuoMatchResultResponse(
        Long duoPostId,
        Long requestId,
        String partnerGameName,
        String partnerTagLine,
        String status
) {
    public static DuoMatchResultResponse from(DuoMatchResultModel readModel) {
        return new DuoMatchResultResponse(
                readModel.getDuoPostId(),
                readModel.getRequestId(),
                readModel.getPartnerGameName(),
                readModel.getPartnerTagLine(),
                readModel.getStatus()
        );
    }
}
