package com.example.lolserver.member.adapter.in.web.response;

import com.example.lolserver.member.application.model.AuthTokenReadModel;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
    public static AuthTokenResponse from(AuthTokenReadModel readModel) {
        return new AuthTokenResponse(
                readModel.accessToken(),
                readModel.refreshToken(),
                readModel.expiresIn()
        );
    }
}
