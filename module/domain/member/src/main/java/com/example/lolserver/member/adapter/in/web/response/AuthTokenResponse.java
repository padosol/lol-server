package com.example.lolserver.member.adapter.in.web.response;

import com.example.lolserver.member.application.model.resultmodel.AuthTokenResultModel;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    public static AuthTokenResponse from(AuthTokenResultModel readModel) {
        return new AuthTokenResponse(
                readModel.accessToken(),
                readModel.refreshToken(),
                readModel.expiresIn()
        );
    }
}
