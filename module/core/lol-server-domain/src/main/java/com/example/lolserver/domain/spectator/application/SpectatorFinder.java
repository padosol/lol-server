package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorClientPort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class SpectatorFinder {

    private final SpectatorCachePort spectatorCachePort;
    private final SpectatorClientPort spectatorClientPort;
    private final SummonerPersistencePort summonerPersistencePort;

    public CurrentGameInfoReadModel getCurrentGameInfo(String puuid, String region) {
        // 1. 게임 정보 캐시 조회
        CurrentGameInfoReadModel cached = spectatorCachePort.findByPuuid(region, puuid);
        if (cached != null) {
            // 캐시 무효화 체크: Summoner.revisionDate > game.gameStartTime
            if (shouldInvalidateCache(puuid, cached)) {
                // 모든 참여자 캐시 삭제
                spectatorCachePort.deleteGameWithAllParticipants(region, cached.gameId());
            } else {
                return cached;
            }
        }

        // 2. Negative Cache 체크 (게임 없음 캐시)
        if (spectatorCachePort.isNoGameCached(region, puuid)) {
            return null;
        }

        // 3. API 호출
        return spectatorClientPort.getCurrentGameInfo(region, puuid);
    }

    /**
     * 캐시 무효화 여부를 판단합니다.
     * Summoner의 갱신 시간(revisionDate)이 게임 시작 시간보다 이후이면 캐시를 무효화합니다.
     */
    private boolean shouldInvalidateCache(String puuid, CurrentGameInfoReadModel gameInfo) {
        return summonerPersistencePort.findById(puuid)
                .map(summoner -> {
                    LocalDateTime gameStartTime = convertToLocalDateTime(gameInfo.gameStartTime());
                    return summoner.getRevisionDate().isAfter(gameStartTime);
                })
                .orElse(false);
    }

    /**
     * Unix Timestamp(밀리초)를 LocalDateTime으로 변환합니다.
     */
    private LocalDateTime convertToLocalDateTime(long timestampMillis) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMillis),
                ZoneId.systemDefault()
        );
    }
}
