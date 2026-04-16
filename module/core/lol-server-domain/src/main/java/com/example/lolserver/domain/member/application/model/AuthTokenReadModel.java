package com.example.lolserver.domain.member.application.model;

public record AuthTokenReadModel(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
