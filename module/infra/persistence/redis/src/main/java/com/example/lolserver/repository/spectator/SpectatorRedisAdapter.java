package com.example.lolserver.repository.spectator;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.model.ParticipantReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpectatorRedisAdapter implements SpectatorCachePort {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "spectator:active_game:";
    private static final String NO_GAME_KEY_PREFIX = "spectator:no_game:";
    private static final String GAME_META_KEY_PREFIX = "spectator:game_meta:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration NO_GAME_TTL = Duration.ofSeconds(30);
    private static final String NO_GAME_VALUE = "NO_GAME";

    @Override
    public CurrentGameInfoReadModel findByPuuid(String platformId, String puuid) {
        String key = buildKey(platformId, puuid);
        log.debug("Attempting to retrieve current game from cache for key: {}", key);
        return (CurrentGameInfoReadModel) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void saveCurrentGame(String platformId, CurrentGameInfoReadModel gameInfo) {
        if (gameInfo == null || gameInfo.participants() == null) {
            return;
        }

        // 모든 참여자의 puuid를 키로 동일한 게임 정보 저장
        for (ParticipantReadModel participant : gameInfo.participants()) {
            String key = buildKey(platformId, participant.puuid());
            log.debug("Caching current game for key: {}", key);
            redisTemplate.opsForValue().set(key, gameInfo, CACHE_TTL);
        }
    }

    @Override
    public void deleteByPuuid(String platformId, String puuid) {
        String key = buildKey(platformId, puuid);
        log.debug("Deleting cache for key: {}", key);
        redisTemplate.delete(key);
    }

    @Override
    public void saveNoGame(String platformId, String puuid) {
        String key = buildNoGameKey(platformId, puuid);
        log.debug("Saving no-game cache for key: {}", key);
        redisTemplate.opsForValue().set(key, NO_GAME_VALUE, NO_GAME_TTL);
    }

    @Override
    public boolean isNoGameCached(String platformId, String puuid) {
        String key = buildNoGameKey(platformId, puuid);
        Boolean hasKey = redisTemplate.hasKey(key);
        log.debug("Checking no-game cache for key: {}, exists: {}", key, hasKey);
        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public void saveGameMeta(String platformId, long gameId, long gameStartTime, List<String> participantPuuids) {
        String key = buildGameMetaKey(platformId, gameId);
        log.debug("Saving game meta for key: {}", key);

        Map<String, Object> metaData = Map.of(
                "gameStartTime", gameStartTime,
                "participantPuuids", participantPuuids
        );
        redisTemplate.opsForValue().set(key, metaData, CACHE_TTL);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deleteGameWithAllParticipants(String platformId, long gameId) {
        String metaKey = buildGameMetaKey(platformId, gameId);
        log.debug("Deleting game with all participants for key: {}", metaKey);

        Object metaData = redisTemplate.opsForValue().get(metaKey);
        if (metaData instanceof Map<?, ?> meta) {
            Object puuidsObj = meta.get("participantPuuids");
            if (puuidsObj instanceof List<?> puuids) {
                for (Object puuid : puuids) {
                    if (puuid instanceof String puuidStr) {
                        String participantKey = buildKey(platformId, puuidStr);
                        redisTemplate.delete(participantKey);
                        log.debug("Deleted participant cache: {}", participantKey);
                    }
                }
            }
        }

        // 메타데이터도 삭제
        redisTemplate.delete(metaKey);
        log.debug("Deleted game meta: {}", metaKey);
    }

    private String buildKey(String platformId, String puuid) {
        return CACHE_KEY_PREFIX + platformId + ":" + puuid;
    }

    private String buildNoGameKey(String platformId, String puuid) {
        return NO_GAME_KEY_PREFIX + platformId + ":" + puuid;
    }

    private String buildGameMetaKey(String platformId, long gameId) {
        return GAME_META_KEY_PREFIX + platformId + ":" + gameId;
    }
}
