package com.example.lolserver.controller.security;

import com.example.lolserver.domain.member.application.port.out.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenPort {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser().verifyWith(secretKey).build();
    }

    @Override
    public String generateAccessToken(Long memberId, String role) {
        return generateToken(memberId, role, jwtProperties.getAccessTokenExpiry() * 1000);
    }

    @Override
    public String generateRefreshToken(Long memberId, String role) {
        return generateToken(memberId, role, jwtProperties.getRefreshTokenExpiry() * 1000);
    }

    @Override
    public TokenInfo parseToken(String token) {
        Claims claims = parseClaims(token);
        return new TokenInfo(
                claims.get("memberId", Long.class),
                claims.get("role", String.class));
    }

    @Override
    public Long getMemberIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("memberId", Long.class);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.");
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    @Override
    public long getAccessTokenExpiry() {
        return jwtProperties.getAccessTokenExpiry();
    }

    @Override
    public long getRefreshTokenExpiry() {
        return jwtProperties.getRefreshTokenExpiry();
    }

    private String generateToken(Long memberId, String role, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claim("memberId", memberId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }
}
