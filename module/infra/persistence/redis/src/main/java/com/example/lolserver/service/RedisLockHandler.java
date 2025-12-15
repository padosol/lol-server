package com.example.lolserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class RedisLockHandler {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    // 락 해제 로직을 담은 Lua 스크립트
    private static final String UNLOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    // RedisScript 객체로 로드
    private final RedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_LUA_SCRIPT, Long.class);

    public boolean acquireLock(String puuid) {
        String lockKey = "user:lock" + puuid;

        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(lockKey, puuid));
    }

    public boolean releaseLock(String puuid) {
        Long result = redisTemplate.execute(
                unlockScript,
                Collections.singletonList(puuid), // KEYS[1]에 해당하는 락 Key
                puuid                           // ARGV[1]에 해당하는 락 획득 클라이언트 ID
        );

        // 1 (성공적으로 삭제됨) 또는 0 (Key가 없거나 Value가 일치하지 않음) 반환
        return result != null && result == 1L;
    }

}
