package com.example.lolserver.repository.member;

import com.example.lolserver.domain.member.application.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RefreshTokenRedisAdapter implements RefreshTokenPort {

    private static final String KEY_PREFIX = "member:refresh-token:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(Long memberId, String refreshToken, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(KEY_PREFIX + memberId, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> find(Long memberId) {
        String token = stringRedisTemplate.opsForValue().get(KEY_PREFIX + memberId);
        return Optional.ofNullable(token);
    }

    @Override
    public void delete(Long memberId) {
        stringRedisTemplate.delete(KEY_PREFIX + memberId);
    }
}
