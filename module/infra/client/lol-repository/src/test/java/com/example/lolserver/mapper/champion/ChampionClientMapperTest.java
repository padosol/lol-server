package com.example.lolserver.mapper.champion;

import com.example.lolserver.domain.champion.domain.ChampionRotate;
import com.example.lolserver.restclient.summoner.model.ChampionInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChampionClientMapperTest {

    private final ChampionClientMapper mapper = ChampionClientMapper.INSTANCE;

    @DisplayName("ChampionInfo를 ChampionRotate 도메인으로 변환한다")
    @Test
    void toDomain_validChampionInfo_returnsChampionRotate() {
        // given
        List<Integer> freeChampionIdsForNewPlayers = List.of(18, 81, 22, 21, 36);
        List<Integer> freeChampionIds = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ChampionInfo championInfo = new ChampionInfo(10, freeChampionIdsForNewPlayers, freeChampionIds);

        // when
        ChampionRotate result = mapper.toDomain(championInfo);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMaxNewPlayerLevel()).isEqualTo(10);
        assertThat(result.getFreeChampionIdsForNewPlayers()).containsExactly(18, 81, 22, 21, 36);
        assertThat(result.getFreeChampionIds()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @DisplayName("빈 챔피언 목록으로 변환해도 정상 동작한다")
    @Test
    void toDomain_emptyLists_returnsEmptyChampionRotate() {
        // given
        ChampionInfo championInfo = new ChampionInfo(10, List.of(), List.of());

        // when
        ChampionRotate result = mapper.toDomain(championInfo);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMaxNewPlayerLevel()).isEqualTo(10);
        assertThat(result.getFreeChampionIdsForNewPlayers()).isEmpty();
        assertThat(result.getFreeChampionIds()).isEmpty();
    }

    @DisplayName("다양한 maxNewPlayerLevel 값을 올바르게 변환한다")
    @Test
    void toDomain_differentMaxLevel_convertsCorrectly() {
        // given
        ChampionInfo championInfo = new ChampionInfo(15, List.of(1), List.of(2));

        // when
        ChampionRotate result = mapper.toDomain(championInfo);

        // then
        assertThat(result.getMaxNewPlayerLevel()).isEqualTo(15);
    }

    @DisplayName("신규 플레이어용 챔피언과 일반 무료 챔피언이 분리된다")
    @Test
    void toDomain_separatesNewPlayerAndFreeChampions() {
        // given
        List<Integer> newPlayerChampions = List.of(100, 101, 102);
        List<Integer> freeChampions = List.of(200, 201, 202, 203);
        ChampionInfo championInfo = new ChampionInfo(10, newPlayerChampions, freeChampions);

        // when
        ChampionRotate result = mapper.toDomain(championInfo);

        // then
        assertThat(result.getFreeChampionIdsForNewPlayers())
                .containsExactly(100, 101, 102)
                .doesNotContainAnyElementsOf(freeChampions);
        assertThat(result.getFreeChampionIds())
                .containsExactly(200, 201, 202, 203)
                .doesNotContainAnyElementsOf(newPlayerChampions);
    }

    @DisplayName("INSTANCE를 통해 매퍼를 가져올 수 있다")
    @Test
    void instance_isNotNull() {
        // then
        assertThat(ChampionClientMapper.INSTANCE).isNotNull();
    }
}
