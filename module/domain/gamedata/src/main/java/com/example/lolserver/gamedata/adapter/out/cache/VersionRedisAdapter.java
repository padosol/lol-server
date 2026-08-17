package com.example.lolserver.gamedata.adapter.out.cache;

import com.example.lolserver.gamedata.application.model.readmodel.VersionReadModel;
import com.example.lolserver.gamedata.application.port.out.VersionCachePort;
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

    /**
     * patchVersionData 가 붙기 전 값이 TTL(24시간) 동안 남아 데이터 버전이 null 로
     * 내려가는 것을 막으려고 키를 올렸다. 옛 키(version:latest)는 TTL 로 알아서 사라진다.
     */
    private static final String CACHE_KEY = "version:latest:v2";
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
