package com.example.lolserver.member.adapter.out.cache;

import com.example.lolserver.member.application.port.out.OAuthAuthorizationRequestPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuthAuthorizationRequestRedisAdapter
        implements OAuthAuthorizationRequestPort {

    private static final String KEY_PREFIX = "oauth:authorization-request:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String state, String serializedRequest, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(KEY_PREFIX + state, serializedRequest,
                        ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Optional<String> find(String state) {
        return Optional.ofNullable(
                stringRedisTemplate.opsForValue().get(KEY_PREFIX + state));
    }

    @Override
    public Optional<String> findAndDelete(String state) {
        return Optional.ofNullable(
                stringRedisTemplate.opsForValue()
                        .getAndDelete(KEY_PREFIX + state));
    }
}
