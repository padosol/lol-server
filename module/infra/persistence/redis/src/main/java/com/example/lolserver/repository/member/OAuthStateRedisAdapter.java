package com.example.lolserver.repository.member;

import com.example.lolserver.domain.member.application.port.out.OAuthStatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuthStateRedisAdapter implements OAuthStatePort {

    private static final String KEY_PREFIX = "oauth:state:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void saveState(String state, long ttlSeconds) {
        stringRedisTemplate.opsForValue()
                .set(KEY_PREFIX + state, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean validateAndDelete(String state) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(KEY_PREFIX + state));
    }
}
