package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.port.SpectatorPort;
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
    private SpectatorPort spectatorRedisAdapter;

    @Mock
    private SpectatorPort spectatorClientAdapter;

    private SpectatorFinder spectatorFinder;

    @BeforeEach
    void setUp() {
        spectatorFinder = new SpectatorFinder(spectatorRedisAdapter, spectatorClientAdapter);
    }

    @DisplayName("Redis에 데이터가 있으면 Redis 결과를 반환하고 Client는 호출하지 않는다")
    @Test
    void getCurrentGameInfo_Redis에데이터있음_Redis결과반환() {
        // given
        String puuid = "test-puuid";
        String region = "kr";
        CurrentGameInfoReadModel redisResult = createGameInfo(12345L);

        given(spectatorRedisAdapter.findAllCurrentGameInfo(puuid, region))
                .willReturn(redisResult);

        // when
        CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isEqualTo(redisResult);
        assertThat(result.gameId()).isEqualTo(12345L);
        then(spectatorRedisAdapter).should().findAllCurrentGameInfo(puuid, region);
        then(spectatorClientAdapter).should(never()).findAllCurrentGameInfo(any(), any());
    }

    @DisplayName("Redis가 null을 반환하면 Client에서 조회한다")
    @Test
    void getCurrentGameInfo_Redis에null_Client결과반환() {
        // given
        String puuid = "test-puuid";
        String region = "kr";
        CurrentGameInfoReadModel clientResult = createGameInfo(67890L);

        given(spectatorRedisAdapter.findAllCurrentGameInfo(puuid, region))
                .willReturn(null);
        given(spectatorClientAdapter.findAllCurrentGameInfo(puuid, region))
                .willReturn(clientResult);

        // when
        CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isEqualTo(clientResult);
        assertThat(result.gameId()).isEqualTo(67890L);
        then(spectatorRedisAdapter).should().findAllCurrentGameInfo(puuid, region);
        then(spectatorClientAdapter).should().findAllCurrentGameInfo(puuid, region);
    }

    @DisplayName("Redis와 Client 모두 null이면 null을 반환한다")
    @Test
    void getCurrentGameInfo_둘다null_null반환() {
        // given
        String puuid = "test-puuid";
        String region = "kr";

        given(spectatorRedisAdapter.findAllCurrentGameInfo(puuid, region))
                .willReturn(null);
        given(spectatorClientAdapter.findAllCurrentGameInfo(puuid, region))
                .willReturn(null);

        // when
        CurrentGameInfoReadModel result = spectatorFinder.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isNull();
        then(spectatorRedisAdapter).should().findAllCurrentGameInfo(puuid, region);
        then(spectatorClientAdapter).should().findAllCurrentGameInfo(puuid, region);
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
