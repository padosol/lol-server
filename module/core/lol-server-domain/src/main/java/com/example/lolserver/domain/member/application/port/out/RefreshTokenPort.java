package com.example.lolserver.domain.member.application.port.out;

import java.util.Optional;

public interface RefreshTokenPort {

    void save(Long memberId, String refreshToken, long ttlSeconds);

    Optional<String> find(Long memberId);

    void delete(Long memberId);
}
