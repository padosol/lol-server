package com.example.lolserver.summoner.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.summoner.application.model.readmodel.CurrentGameInfoReadModel;
import com.example.lolserver.summoner.application.port.out.SummonerPersistencePort;
import com.example.lolserver.summoner.domain.Summoner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SpectatorServiceTest {

    @Mock
    private SpectatorFinder spectatorFinder;

    @Mock
    private SummonerPersistencePort summonerPersistencePort;

    @InjectMocks
    private SpectatorService spectatorService;

    @DisplayName("puuid 만으로 조회하면 저장된 소환사의 platformId 를 쓴다")
    @Test
    void getCurrentGameInfo_puuid_저장된플랫폼() {
        // given
        given(summonerPersistencePort.findById("puuid-1"))
                .willReturn(Optional.of(Summoner.builder().puuid("puuid-1").platformId("kr").build()));

        // when
        spectatorService.getCurrentGameInfo("puuid-1");

        // then
        then(spectatorFinder).should().getCurrentGameInfo("puuid-1", "kr");
    }

    @DisplayName("플랫폼이 없는 소환사는 UNKNOWN_SUMMONER_PLATFORM")
    @Test
    void getCurrentGameInfo_puuid_플랫폼없음() {
        // given
        given(summonerPersistencePort.findById("puuid-1"))
                .willReturn(Optional.of(Summoner.builder().puuid("puuid-1").build()));

        // when & then
        assertThatThrownBy(() -> spectatorService.getCurrentGameInfo("puuid-1"))
                .isInstanceOf(CoreException.class)
                .extracting(e -> ((CoreException) e).getErrorType())
                .isEqualTo(ErrorType.UNKNOWN_SUMMONER_PLATFORM);
        then(spectatorFinder).shouldHaveNoInteractions();
    }

    @DisplayName("현재 게임 정보가 존재하면 게임 정보를 반환한다")
    @Test
    void getCurrentGameInfo_데이터존재_게임정보반환() {
        // given
        String puuid = "test-puuid-123";
        String platformId = "kr";

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
        given(spectatorFinder.getCurrentGameInfo(puuid, platformId)).willReturn(gameInfo);

        // when
        CurrentGameInfoReadModel result = spectatorService.getCurrentGameInfo(puuid, platformId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.gameId()).isEqualTo(12345L);
        assertThat(result.gameType()).isEqualTo("MATCHED_GAME");
        assertThat(result.gameMode()).isEqualTo("CLASSIC");
        assertThat(result.platformId()).isEqualTo("KR");
        then(spectatorFinder).should().getCurrentGameInfo(puuid, platformId);
    }

    @DisplayName("현재 진행 중인 게임이 없으면 null을 반환한다")
    @Test
    void getCurrentGameInfo_데이터없음_null반환() {
        // given
        String puuid = "test-puuid-no-game";
        String platformId = "kr";

        given(spectatorFinder.getCurrentGameInfo(puuid, platformId)).willReturn(null);

        // when
        CurrentGameInfoReadModel result = spectatorService.getCurrentGameInfo(puuid, platformId);

        // then
        assertThat(result).isNull();
        then(spectatorFinder).should().getCurrentGameInfo(puuid, platformId);
    }
}
