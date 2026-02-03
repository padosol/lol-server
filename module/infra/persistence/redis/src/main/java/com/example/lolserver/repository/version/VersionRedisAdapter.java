package com.example.lolserver.repository.version;

import com.example.lolserver.domain.version.application.model.VersionReadModel;
import com.example.lolserver.domain.version.application.port.out.VersionCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class VersionRedisAdapter implements VersionCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "version:latest";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @Override
    public VersionReadModel findLatestVersion() {
        log.debug("Attempting to retrieve latest version from cache");
        return (VersionReadModel) redisTemplate.opsForValue().get(CACHE_KEY);
    }

    @Override
    public void saveLatestVersion(VersionReadModel versionReadModel) {
        if (versionReadModel == null) {
            return;
        }
        log.debug("Caching latest version: {}", versionReadModel.versionValue());
        redisTemplate.opsForValue().set(CACHE_KEY, versionReadModel, CACHE_TTL);
    }
}
