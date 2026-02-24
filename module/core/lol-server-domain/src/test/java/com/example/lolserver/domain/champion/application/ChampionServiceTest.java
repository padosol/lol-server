package com.example.lolserver.domain.champion.application;

import com.example.lolserver.domain.champion.application.port.out.ChampionClientPort;
import com.example.lolserver.domain.champion.application.port.out.ChampionPersistencePort;
import com.example.lolserver.domain.champion.domain.ChampionRotate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChampionServiceTest {

    @Mock
    private ChampionClientPort championClientPort;

    @Mock
    private ChampionPersistencePort championPersistencePort;

    @InjectMocks
    private ChampionService championService;

    @DisplayName("캐시에 챔피언 로테이션이 존재하면 캐시된 데이터를 반환한다")
    @Test
    void getChampionRotate_캐시존재_캐시반환() {
        // given
        String platformId = "kr";
        ChampionRotate cachedRotate = new ChampionRotate(
                10,
                List.of(18, 81, 22),
                List.of(1, 2, 3, 4, 5)
        );
        given(championPersistencePort.getChampionRotate(platformId)).willReturn(Optional.of(cachedRotate));

        // when
        ChampionRotate result = championService.getChampionRotate(platformId);

        // then
        assertThat(result).isEqualTo(cachedRotate);
        assertThat(result.getMaxNewPlayerLevel()).isEqualTo(10);
        then(championPersistencePort).should().getChampionRotate(platformId);
        then(championClientPort).should(never()).getChampionRotate(platformId);
    }

    @DisplayName("캐시에 챔피언 로테이션이 없으면 클라이언트에서 조회 후 저장한다")
    @Test
    void getChampionRotate_캐시없음_클라이언트조회후저장() {
        // given
        String platformId = "kr";
        ChampionRotate newRotate = new ChampionRotate(
                10,
                List.of(18, 81, 22, 21, 36),
                List.of(6, 7, 8, 9, 10, 11, 12)
        );
        given(championPersistencePort.getChampionRotate(platformId)).willReturn(Optional.empty());
        given(championClientPort.getChampionRotate(platformId)).willReturn(newRotate);

        // when
        ChampionRotate result = championService.getChampionRotate(platformId);

        // then
        assertThat(result).isEqualTo(newRotate);
        assertThat(result.getFreeChampionIds()).hasSize(7);
        then(championPersistencePort).should().getChampionRotate(platformId);
        then(championClientPort).should().getChampionRotate(platformId);
        then(championPersistencePort).should().saveChampionRotate(platformId, newRotate);
    }

    @DisplayName("다른 지역의 챔피언 로테이션을 조회할 수 있다")
    @Test
    void getChampionRotate_다른지역_정상조회() {
        // given
        String platformId = "na1";
        ChampionRotate naRotate = new ChampionRotate(
                10,
                List.of(18, 81, 22),
                List.of(100, 101, 102, 103)
        );
        given(championPersistencePort.getChampionRotate(platformId)).willReturn(Optional.of(naRotate));

        // when
        ChampionRotate result = championService.getChampionRotate(platformId);

        // then
        assertThat(result).isEqualTo(naRotate);
        assertThat(result.getFreeChampionIds()).containsExactly(100, 101, 102, 103);
        then(championPersistencePort).should().getChampionRotate(platformId);
    }
}
