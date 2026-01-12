package com.example.lolserver.adapter.champion;

import com.example.lolserver.domain.champion.domain.ChampionRotate;
import com.example.lolserver.mapper.champion.ChampionClientMapper;
import com.example.lolserver.restclient.summoner.ChampionRotateRestClient;
import com.example.lolserver.restclient.summoner.model.ChampionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChampionClientAdapterTest {

    @Mock
    private ChampionRotateRestClient championRotateRestClient;

    @Mock
    private ChampionClientMapper championClientMapper;

    private ChampionClientAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ChampionClientAdapter(championRotateRestClient, championClientMapper);
    }

    @DisplayName("지역으로 챔피언 로테이션을 조회하면 도메인 객체를 반환한다")
    @Test
    void getChampionRotate_validRegion_returnsChampionRotate() {
        // given
        String region = "kr";
        List<Integer> freeChampionIdsForNewPlayers = List.of(18, 81, 22);
        List<Integer> freeChampionIds = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        ChampionInfo championInfo = new ChampionInfo(10, freeChampionIdsForNewPlayers, freeChampionIds);
        ChampionRotate expectedRotate = new ChampionRotate(10, freeChampionIdsForNewPlayers, freeChampionIds);

        given(championRotateRestClient.getChampionInfo(region)).willReturn(championInfo);
        given(championClientMapper.toDomain(championInfo)).willReturn(expectedRotate);

        // when
        ChampionRotate result = adapter.getChampionRotate(region);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMaxNewPlayerLevel()).isEqualTo(10);
        assertThat(result.getFreeChampionIds()).hasSize(10);
        assertThat(result.getFreeChampionIdsForNewPlayers()).hasSize(3);
        then(championRotateRestClient).should().getChampionInfo(region);
        then(championClientMapper).should().toDomain(championInfo);
    }

    @DisplayName("다른 지역의 챔피언 로테이션을 조회할 수 있다")
    @Test
    void getChampionRotate_differentRegion_returnsChampionRotate() {
        // given
        String region = "na1";
        List<Integer> freeChampionIdsForNewPlayers = List.of(20, 30, 40);
        List<Integer> freeChampionIds = List.of(100, 101, 102, 103, 104);

        ChampionInfo championInfo = new ChampionInfo(10, freeChampionIdsForNewPlayers, freeChampionIds);
        ChampionRotate expectedRotate = new ChampionRotate(10, freeChampionIdsForNewPlayers, freeChampionIds);

        given(championRotateRestClient.getChampionInfo(region)).willReturn(championInfo);
        given(championClientMapper.toDomain(championInfo)).willReturn(expectedRotate);

        // when
        ChampionRotate result = adapter.getChampionRotate(region);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getFreeChampionIds()).containsExactly(100, 101, 102, 103, 104);
        then(championRotateRestClient).should().getChampionInfo(region);
    }

    @DisplayName("신규 유저용 무료 챔피언 목록과 일반 무료 챔피언 목록이 다를 수 있다")
    @Test
    void getChampionRotate_differentChampionLists_returnsBothLists() {
        // given
        String region = "kr";
        List<Integer> newPlayerChampions = List.of(1, 2, 3);
        List<Integer> freeChampions = List.of(10, 20, 30, 40, 50);

        ChampionInfo championInfo = new ChampionInfo(10, newPlayerChampions, freeChampions);
        ChampionRotate expectedRotate = new ChampionRotate(10, newPlayerChampions, freeChampions);

        given(championRotateRestClient.getChampionInfo(region)).willReturn(championInfo);
        given(championClientMapper.toDomain(championInfo)).willReturn(expectedRotate);

        // when
        ChampionRotate result = adapter.getChampionRotate(region);

        // then
        assertThat(result.getFreeChampionIdsForNewPlayers()).containsExactly(1, 2, 3);
        assertThat(result.getFreeChampionIds()).containsExactly(10, 20, 30, 40, 50);
        assertThat(result.getFreeChampionIdsForNewPlayers())
                .isNotEqualTo(result.getFreeChampionIds());
    }
}
