package com.example.lolserver.member.application.model.resultmodel;

public record AuthTokenResultModel(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
