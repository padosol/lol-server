package com.example.lolserver.member.application.model;

public record AuthTokenReadModel(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
