package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorClientPort;
import com.example.lolserver.domain.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.domain.summoner.domain.Summoner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("SpectatorFinder 테스트")
@ExtendWith(MockitoExtension.class)
class SpectatorFinderTest {

    @Mock
    private SpectatorCachePort spectatorCachePort;

    @Mock
    private SpectatorClientPort spectatorClientPort;

    @Mock
    private SummonerPersistencePort summonerPersistencePort;

    private SpectatorFinder spectatorFinder;

    @BeforeEach
    void setUp() {
        spectatorFinder = new SpectatorFinder(spectatorCachePort, spectatorClientPort, summonerPersistencePort);
    }

    @Nested
    @DisplayName("캐시 히트 테스트")
    class CacheHitTest {

        @DisplayName("캐시에 데이터가 있고 유효하면 캐시 결과를 반환하고 Client는 호출하지 않는다")
        @Test
        void getCurrentGameInfo_캐시에데이터있고유효함_캐시결과반환() {
            // given
            String puuid = "test-puuid";
            String region = "kr";
            long gameStartTime = System.currentTimeMillis();
            CurrentGameInfoReadModel cachedResult = createGameInfo(12345L, gameStartTime);
            Summoner summoner = createSummoner(puuid, toLocalDateTime(gameStartTime).minusMinutes(10));

            given(spectatorCachePort.findByPuuid(region, puuid))
                    .willReturn(cachedResult);
            given(summonerPersistencePort.findById(puuid))
                    .willReturn(Optional.of(summoner));

            // when
            CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

            // then
            assertThat(result).isEqualTo(cachedResult);
            assertThat(result.gameId()).isEqualTo(12345L);
            then(spectatorCachePort).should().findByPuuid(region, puuid);
            then(spectatorClientPort).should(never()).getCurrentGameInfo(any(), any());
            then(spectatorCachePort).should(never()).deleteGameWithAllParticipants(any(), anyLong());
        }

        @DisplayName("캐시에 데이터가 있지만 revisionDate가 gameStartTime 이후면 캐시 무효화 후 API 호출")
        @Test
        void getCurrentGameInfo_캐시무효화_API호출() {
            // given
            String puuid = "test-puuid";
            String region = "kr";
            long gameStartTime = System.currentTimeMillis() - 3600_000; // 1시간 전
            CurrentGameInfoReadModel cachedResult = createGameInfo(12345L, gameStartTime);
            Summoner summoner = createSummoner(puuid, LocalDateTime.now()); // 현재 시간 (게임 시작 이후)
            CurrentGameInfoReadModel apiResult = createGameInfo(67890L, System.currentTimeMillis());

            given(spectatorCachePort.findByPuuid(region, puuid))
                    .willReturn(cachedResult);
            given(summonerPersistencePort.findById(puuid))
                    .willReturn(Optional.of(summoner));
            given(spectatorCachePort.isNoGameCached(region, puuid))
                    .willReturn(false);
            given(spectatorClientPort.getCurrentGameInfo(region, puuid))
                    .willReturn(apiResult);

            // when
            CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

            // then
            assertThat(result).isEqualTo(apiResult);
            then(spectatorCachePort).should().deleteGameWithAllParticipants(region, 12345L);
            then(spectatorClientPort).should().getCurrentGameInfo(region, puuid);
        }

        @DisplayName("Summoner가 존재하지 않으면 캐시 결과 그대로 반환")
        @Test
        void getCurrentGameInfo_Summoner없음_캐시결과반환() {
            // given
            String puuid = "test-puuid";
            String region = "kr";
            CurrentGameInfoReadModel cachedResult = createGameInfo(12345L, System.currentTimeMillis());

            given(spectatorCachePort.findByPuuid(region, puuid))
                    .willReturn(cachedResult);
            given(summonerPersistencePort.findById(puuid))
                    .willReturn(Optional.empty());

            // when
            CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

            // then
            assertThat(result).isEqualTo(cachedResult);
            then(spectatorCachePort).should(never()).deleteGameWithAllParticipants(any(), anyLong());
            then(spectatorClientPort).should(never()).getCurrentGameInfo(any(), any());
        }
    }

    @Nested
    @DisplayName("캐시 미스 테스트")
    class CacheMissTest {

        @DisplayName("캐시가 null이고 Negative Cache도 없으면 Client에서 조회한다")
        @Test
        void getCurrentGameInfo_캐시없음_Client결과반환() {
            // given
            String puuid = "test-puuid";
            String region = "kr";
            CurrentGameInfoReadModel clientResult = createGameInfo(67890L, System.currentTimeMillis());

            given(spectatorCachePort.findByPuuid(region, puuid))
                    .willReturn(null);
            given(spectatorCachePort.isNoGameCached(region, puuid))
                    .willReturn(false);
            given(spectatorClientPort.getCurrentGameInfo(region, puuid))
                    .willReturn(clientResult);

            // when
            CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

            // then
            assertThat(result).isEqualTo(clientResult);
            assertThat(result.gameId()).isEqualTo(67890L);
            then(spectatorCachePort).should().findByPuuid(region, puuid);
            then(spectatorCachePort).should().isNoGameCached(region, puuid);
            then(spectatorClientPort).should().getCurrentGameInfo(region, puuid);
        }

        @DisplayName("캐시와 Client 모두 null이면 null을 반환한다")
        @Test
        void getCurrentGameInfo_둘다null_null반환() {
            // given
            String puuid = "test-puuid";
            String region = "kr";

            given(spectatorCachePort.findByPuuid(region, puuid))
                    .willReturn(null);
            given(spectatorCachePort.isNoGameCached(region, puuid))
                    .willReturn(false);
            given(spectatorClientPort.getCurrentGameInfo(region, puuid))
                    .willReturn(null);

            // when
            CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

            // then
            assertThat(result).isNull();
            then(spectatorCachePort).should().findByPuuid(region, puuid);
            then(spectatorClientPort).should().getCurrentGameInfo(region, puuid);
        }
    }

    @Nested
    @DisplayName("Negative Cache 테스트")
    class NegativeCacheTest {

        @DisplayName("Negative Cache가 있으면 API 호출 없이 null 반환")
        @Test
        void getCurrentGameInfo_NegativeCache있음_null반환() {
            // given
            String puuid = "test-puuid";
            String region = "kr";

            given(spectatorCachePort.findByPuuid(region, puuid))
                    .willReturn(null);
            given(spectatorCachePort.isNoGameCached(region, puuid))
                    .willReturn(true);

            // when
            CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

            // then
            assertThat(result).isNull();
            then(spectatorCachePort).should().findByPuuid(region, puuid);
            then(spectatorCachePort).should().isNoGameCached(region, puuid);
            then(spectatorClientPort).should(never()).getCurrentGameInfo(any(), any());
        }
    }

    private CurrentGameInfoReadModel createGameInfo(long gameId, long gameStartTime) {
        return new CurrentGameInfoReadModel(
                gameId,
                "MATCHED_GAME",
                "CLASSIC",
                11L,
                420L,
                gameStartTime,
                600L,
                "KR",
                "encryption-key",
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private Summoner createSummoner(String puuid, LocalDateTime revisionDate) {
        Summoner summoner = new Summoner();
        summoner.setPuuid(puuid);
        summoner.setGameName("TestPlayer");
        summoner.setTagLine("KR1");
        summoner.setRevisionDate(revisionDate);
        summoner.setLastRiotCallDate(revisionDate);
        return summoner;
    }

    private LocalDateTime toLocalDateTime(long timestampMillis) {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMillis),
                ZoneId.systemDefault()
        );
    }
}
