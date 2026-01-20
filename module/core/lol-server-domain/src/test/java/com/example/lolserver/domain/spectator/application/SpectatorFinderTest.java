package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorClientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private SpectatorFinder spectatorFinder;

    @BeforeEach
    void setUp() {
        spectatorFinder = new SpectatorFinder(spectatorCachePort, spectatorClientPort);
    }

    @DisplayName("캐시에 데이터가 있으면 캐시 결과를 반환하고 Client는 호출하지 않는다")
    @Test
    void getCurrentGameInfo_캐시에데이터있음_캐시결과반환() {
        // given
        String puuid = "test-puuid";
        String region = "kr";
        CurrentGameInfoReadModel cachedResult = createGameInfo(12345L);

        given(spectatorCachePort.findByPuuid(region, puuid))
                .willReturn(cachedResult);

        // when
        CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isEqualTo(cachedResult);
        assertThat(result.gameId()).isEqualTo(12345L);
        then(spectatorCachePort).should().findByPuuid(region, puuid);
        then(spectatorClientPort).should(never()).getCurrentGameInfo(any(), any());
    }

    @DisplayName("캐시가 null을 반환하면 Client에서 조회한다")
    @Test
    void getCurrentGameInfo_캐시에null_Client결과반환() {
        // given
        String puuid = "test-puuid";
        String region = "kr";
        CurrentGameInfoReadModel clientResult = createGameInfo(67890L);

        given(spectatorCachePort.findByPuuid(region, puuid))
                .willReturn(null);
        given(spectatorClientPort.getCurrentGameInfo(region, puuid))
                .willReturn(clientResult);

        // when
        CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isEqualTo(clientResult);
        assertThat(result.gameId()).isEqualTo(67890L);
        then(spectatorCachePort).should().findByPuuid(region, puuid);
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
        given(spectatorClientPort.getCurrentGameInfo(region, puuid))
                .willReturn(null);

        // when
        CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isNull();
        then(spectatorCachePort).should().findByPuuid(region, puuid);
        then(spectatorClientPort).should().getCurrentGameInfo(region, puuid);
    }

    private CurrentGameInfoReadModel createGameInfo(long gameId) {
        return new CurrentGameInfoReadModel(
                gameId,
                "MATCHED_GAME",
                "CLASSIC",
                11L,
                System.currentTimeMillis(),
                600L,
                "KR",
                "encryption-key",
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
