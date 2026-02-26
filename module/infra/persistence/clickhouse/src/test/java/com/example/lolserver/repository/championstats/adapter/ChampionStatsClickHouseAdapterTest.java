package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChampionStatsClickHouseAdapterTest {

    @Mock
    private JdbcTemplate clickHouseJdbcTemplate;

    private ChampionStatsClickHouseAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ChampionStatsClickHouseAdapter(clickHouseJdbcTemplate);
    }

    @DisplayName("챔피언 승률 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionWinRates() {
        // given
        List<ChampionWinRateReadModel> expected = List.of(
            new ChampionWinRateReadModel("MIDDLE", 1000, 520, 0.52)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionWinRateReadModel> result = adapter.getChampionWinRates(13, "16.1", "KR", "EMERALD");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).teamPosition()).isEqualTo("MIDDLE");
        assertThat(result.get(0).totalWinRate()).isEqualTo(0.52);
    }

    @DisplayName("챔피언 매치업 통계를 포지션별 Map으로 반환한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionMatchups() {
        // given
        List<AbstractMap.SimpleEntry<String, ChampionMatchupReadModel>> entries = List.of(
            new AbstractMap.SimpleEntry<>("MIDDLE", new ChampionMatchupReadModel(238, 200, 110, 0.55))
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(entries);

        // when
        Map<String, List<ChampionMatchupReadModel>> result = adapter.getChampionMatchups(13, "16.1", "KR", "EMERALD");

        // then
        assertThat(result).containsKey("MIDDLE");
        assertThat(result.get("MIDDLE")).hasSize(1);
        assertThat(result.get("MIDDLE").get(0).opponentChampionId()).isEqualTo(238);
    }

    @DisplayName("챔피언 아이템 빌드 통계를 포지션별 Map으로 반환한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionItemBuilds() {
        // given
        List<AbstractMap.SimpleEntry<String, ChampionItemBuildReadModel>> entries = List.of(
            new AbstractMap.SimpleEntry<>("MIDDLE", new ChampionItemBuildReadModel("[3089,3157]", 500, 260, 0.52))
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(entries);

        // when
        Map<String, List<ChampionItemBuildReadModel>> result = adapter.getChampionItemBuilds(13, "16.1", "KR", "EMERALD");

        // then
        assertThat(result).containsKey("MIDDLE");
        assertThat(result.get("MIDDLE")).hasSize(1);
        assertThat(result.get("MIDDLE").get(0).itemsSorted()).isEqualTo("[3089,3157]");
    }

    @DisplayName("챔피언 룬 빌드 통계를 포지션별 Map으로 반환한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionRuneBuilds() {
        // given
        List<AbstractMap.SimpleEntry<String, ChampionRuneBuildReadModel>> entries = List.of(
            new AbstractMap.SimpleEntry<>("MIDDLE", new ChampionRuneBuildReadModel(8100, "[8112,8139]", 8300, "[8304,8345]", 300, 160, 0.5333))
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(entries);

        // when
        Map<String, List<ChampionRuneBuildReadModel>> result = adapter.getChampionRuneBuilds(13, "16.1", "KR", "EMERALD");

        // then
        assertThat(result).containsKey("MIDDLE");
        assertThat(result.get("MIDDLE")).hasSize(1);
        assertThat(result.get("MIDDLE").get(0).primaryStyleId()).isEqualTo(8100);
    }

    @DisplayName("챔피언 스킬 빌드 통계를 포지션별 Map으로 반환한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionSkillBuilds() {
        // given
        List<AbstractMap.SimpleEntry<String, ChampionSkillBuildReadModel>> entries = List.of(
            new AbstractMap.SimpleEntry<>("MIDDLE", new ChampionSkillBuildReadModel("QWEQEEREQEQWWWW", 400, 210, 0.525))
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(entries);

        // when
        Map<String, List<ChampionSkillBuildReadModel>> result = adapter.getChampionSkillBuilds(13, "16.1", "KR", "EMERALD");

        // then
        assertThat(result).containsKey("MIDDLE");
        assertThat(result.get("MIDDLE")).hasSize(1);
        assertThat(result.get("MIDDLE").get(0).skillOrder15()).isEqualTo("QWEQEEREQEQWWWW");
    }
}
