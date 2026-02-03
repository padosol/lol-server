package com.example.lolserver.adapter.spectator;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.model.ParticipantReadModel;
import com.example.lolserver.domain.spectator.application.port.out.SpectatorCachePort;
import com.example.lolserver.mapper.spectator.SpectatorClientMapper;
import com.example.lolserver.restclient.spectator.SpectatorRestClient;
import com.example.lolserver.restclient.spectator.model.CurrentGameInfoVO;
import com.example.lolserver.restclient.spectator.model.ParticipantVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SpectatorClientAdapterTest {

    @Mock
    private SpectatorRestClient spectatorRestClient;

    @Mock
    private SpectatorClientMapper spectatorClientMapper;

    @Mock
    private SpectatorCachePort spectatorCachePort;

    private SpectatorClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpectatorClientAdapter(spectatorRestClient, spectatorClientMapper, spectatorCachePort);
    }

    @DisplayName("API 조회 성공 시 결과를 반환하고 캐시 및 메타데이터를 저장한다")
    @Test
    void getCurrentGameInfo_success_returnsAndCachesWithMeta() {
        // given
        String region = "kr";
        String puuid = "test-puuid";
        long gameStartTime = System.currentTimeMillis();
        CurrentGameInfoVO vo = createVO(12345L, gameStartTime);
        CurrentGameInfoReadModel readModel = createReadModel(12345L, gameStartTime);

        given(spectatorRestClient.getCurrentGameInfoByPuuid(region, puuid)).willReturn(vo);
        given(spectatorClientMapper.toReadModel(vo)).willReturn(readModel);

        // when
        CurrentGameInfoReadModel result = adapter.getCurrentGameInfo(region, puuid);

        // then
        assertThat(result).isEqualTo(readModel);
        assertThat(result.gameId()).isEqualTo(12345L);
        then(spectatorRestClient).should().getCurrentGameInfoByPuuid(region, puuid);
        then(spectatorClientMapper).should().toReadModel(vo);
        then(spectatorCachePort).should().saveCurrentGame(region, readModel);
        then(spectatorCachePort).should().saveGameMeta(region, 12345L, gameStartTime, List.of("puuid-1"));
    }

    @DisplayName("API가 null을 반환하면 Negative Cache 저장 후 null 반환")
    @Test
    void getCurrentGameInfo_apiReturnsNull_savesNegativeCache() {
        // given
        String region = "kr";
        String puuid = "test-puuid";

        given(spectatorRestClient.getCurrentGameInfoByPuuid(region, puuid)).willReturn(null);

        // when
        CurrentGameInfoReadModel result = adapter.getCurrentGameInfo(region, puuid);

        // then
        assertThat(result).isNull();
        then(spectatorRestClient).should().getCurrentGameInfoByPuuid(region, puuid);
        then(spectatorClientMapper).should(never()).toReadModel(any(CurrentGameInfoVO.class));
        then(spectatorCachePort).should(never()).saveCurrentGame(any(), any());
        then(spectatorCachePort).should().saveNoGame(region, puuid);
    }

    @DisplayName("API 예외 발생 시 null 반환")
    @Test
    void getCurrentGameInfo_apiThrows_returnsNull() {
        // given
        String region = "kr";
        String puuid = "test-puuid";

        given(spectatorRestClient.getCurrentGameInfoByPuuid(region, puuid))
                .willThrow(new RuntimeException("API Error"));

        // when
        CurrentGameInfoReadModel result = adapter.getCurrentGameInfo(region, puuid);

        // then
        assertThat(result).isNull();
        then(spectatorCachePort).should(never()).saveCurrentGame(any(), any());
    }

    private CurrentGameInfoVO createVO(long gameId, long gameStartTime) {
        return new CurrentGameInfoVO(
                gameId, "MATCHED_GAME", "CLASSIC", 11L,
                gameStartTime, 600L, "KR", "key",
                List.of(new ParticipantVO("Player1", "puuid-1", 1L, 100L, 4L, 7L, false, null)),
                Collections.emptyList()
        );
    }

    private CurrentGameInfoReadModel createReadModel(long gameId, long gameStartTime) {
        return new CurrentGameInfoReadModel(
                gameId, "MATCHED_GAME", "CLASSIC", 11L,
                gameStartTime, 600L, "KR", "key",
                List.of(new ParticipantReadModel("Player1", "puuid-1", 1L, 100L, 4L, 7L, false, null)),
                Collections.emptyList()
        );
    }
}
