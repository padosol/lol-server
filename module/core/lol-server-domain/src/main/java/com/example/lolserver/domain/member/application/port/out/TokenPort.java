package com.example.lolserver.domain.member.application.port.out;

public interface TokenPort {

    record TokenInfo(Long memberId, String role) {}

    String generateAccessToken(Long memberId, String role);

    String generateRefreshToken(Long memberId, String role);

    TokenInfo parseToken(String token);

    Long getMemberIdFromToken(String token);

    boolean validateToken(String token);

    long getAccessTokenExpiry();

    long getRefreshTokenExpiry();

    String getRoleFromToken(String token);
}
