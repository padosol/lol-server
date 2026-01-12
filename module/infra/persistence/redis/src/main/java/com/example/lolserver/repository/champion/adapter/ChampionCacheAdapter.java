package com.example.lolserver.repository.champion.adapter;

import com.example.lolserver.domain.champion.application.port.out.ChampionPersistencePort;
import com.example.lolserver.domain.champion.domain.ChampionRotate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChampionCacheAdapter implements ChampionPersistencePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "champion_rotation_";
    private static final Duration CACHE_TTL = Duration.ofHours(1); // Cache for 1 hour

    @Override
    public Optional<ChampionRotate> getChampionRotate(String region) {
        log.info("Attempting to retrieve champion rotation from cache for region: {}", region);
        return Optional.ofNullable((ChampionRotate) redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + region));
    }

    @Override
    public void saveChampionRotate(String region, ChampionRotate championRotate) {
        log.info("Caching champion rotation for region: {}", region);
        redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + region, championRotate, CACHE_TTL);
    }
}
