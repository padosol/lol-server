package com.example.lolserver.domain.spectator.application;

import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SpectatorServiceTest {

    @Mock
    private SpectatorFinder spectatorFinder;

    @InjectMocks
    private SpectatorService spectatorService;

    @DisplayName("현재 게임 정보가 존재하면 게임 정보를 반환한다")
    @Test
    void getCurrentGameInfo_데이터존재_게임정보반환() {
        // given
        String puuid = "test-puuid-123";
        String region = "kr";

        CurrentGameInfoReadModel gameInfo = new CurrentGameInfoReadModel(
                12345L,
                "MATCHED_GAME",
                "CLASSIC",
                11L,
                420L,
                System.currentTimeMillis(),
                600L,
                "KR",
                "encryption-key-123",
                Collections.emptyList(),
                Collections.emptyList()
        );
        given(spectatorFinder.getCurrentGameInfo(puuid, region)).willReturn(gameInfo);

        // when
        CurrentGameInfoReadModel result = spectatorService.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isNotNull();
        assertThat(result.gameId()).isEqualTo(12345L);
        assertThat(result.gameType()).isEqualTo("MATCHED_GAME");
        assertThat(result.gameMode()).isEqualTo("CLASSIC");
        assertThat(result.platformId()).isEqualTo("KR");
        then(spectatorFinder).should().getCurrentGameInfo(puuid, region);
    }

    @DisplayName("현재 진행 중인 게임이 없으면 null을 반환한다")
    @Test
    void getCurrentGameInfo_데이터없음_null반환() {
        // given
        String puuid = "test-puuid-no-game";
        String region = "kr";

        given(spectatorFinder.getCurrentGameInfo(puuid, region)).willReturn(null);

        // when
        CurrentGameInfoReadModel result = spectatorService.getCurrentGameInfo(puuid, region);

        // then
        assertThat(result).isNull();
        then(spectatorFinder).should().getCurrentGameInfo(puuid, region);
    }
}
