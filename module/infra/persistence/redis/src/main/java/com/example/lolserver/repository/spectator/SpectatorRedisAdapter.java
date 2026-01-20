package com.example.lolserver.repository.spectator;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.model.ParticipantReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpectatorRedisAdapter implements SpectatorCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "spectator:active_game:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public CurrentGameInfoReadModel findByPuuid(String region, String puuid) {
        String key = buildKey(region, puuid);
        log.debug("Attempting to retrieve current game from cache for key: {}", key);
        return (CurrentGameInfoReadModel) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void saveCurrentGame(String region, CurrentGameInfoReadModel gameInfo) {
        if (gameInfo == null || gameInfo.participants() == null) {
            return;
        }

        // 모든 참여자의 puuid를 키로 동일한 게임 정보 저장
        for (ParticipantReadModel participant : gameInfo.participants()) {
            String key = buildKey(region, participant.puuid());
            log.debug("Caching current game for key: {}", key);
            redisTemplate.opsForValue().set(key, gameInfo, CACHE_TTL);
        }
    }

    @Override
    public void deleteByPuuid(String region, String puuid) {
        // 추후 구현 예정
        log.debug("deleteByPuuid is not yet implemented. region: {}, puuid: {}", region, puuid);
    }

    private String buildKey(String region, String puuid) {
        return CACHE_KEY_PREFIX + region + ":" + puuid;
    }
}
