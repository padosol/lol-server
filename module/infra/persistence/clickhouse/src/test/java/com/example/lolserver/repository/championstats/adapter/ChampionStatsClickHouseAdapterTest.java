package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.ChampionBootBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
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

    @DisplayName("챔피언의 모든 포지션 승률 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionWinRates() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionWinRateReadModel> expected = List.of(
            new ChampionWinRateReadModel("MIDDLE", 1000, 520, 0.52),
            new ChampionWinRateReadModel("TOP", 200, 110, 0.55)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionWinRateReadModel> result = adapter.getChampionWinRates(13, "16.1", "KR", tierFilter);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).teamPosition()).isEqualTo("MIDDLE");
        assertThat(result.get(0).totalGames()).isEqualTo(1000);
        assertThat(result.get(1).teamPosition()).isEqualTo("TOP");
        assertThat(result.get(1).totalGames()).isEqualTo(200);
    }

    @DisplayName("매치업을 TOP(승률 높은 순) + BOTTOM(승률 낮은 순)으로 합쳐서 반환한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionMatchups() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionMatchupReadModel> top = List.of(
            ChampionMatchupReadModel.top(7, 120, 0.5417, 0.12),
            ChampionMatchupReadModel.top(103, 100, 0.5300, 0.10)
        );
        List<ChampionMatchupReadModel> bottom = List.of(
            ChampionMatchupReadModel.bottom(238, 150, 0.4667, 0.15),
            ChampionMatchupReadModel.bottom(91, 130, 0.4692, 0.13)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(top, bottom);

        // when
        List<ChampionMatchupReadModel> result = adapter.getChampionMatchups(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(result).hasSize(4);
        assertThat(result.get(0).rankType()).isEqualTo("TOP");
        assertThat(result.get(0).opponentChampionId()).isEqualTo(7);
        assertThat(result.get(2).rankType()).isEqualTo("BOTTOM");
        assertThat(result.get(2).opponentChampionId()).isEqualTo(238);
    }

    @DisplayName("챔피언 룬 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionRuneBuilds() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionRuneBuildReadModel> expected = List.of(
            new ChampionRuneBuildReadModel(8100, 8300, 8112, 8139, 8143, 8135, 8304, 8345, 300, 0.5333, 0.6)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionRuneBuildReadModel> result = adapter.getChampionRuneBuilds(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).primaryStyleId()).isEqualTo(8100);
        assertThat(result.get(0).primaryPerk0()).isEqualTo(8112);
    }

    @DisplayName("챔피언 소환사 주문 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionSpellStats() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionSpellStatsReadModel> expected = List.of(
            new ChampionSpellStatsReadModel(4, 14, 800, 0.52, 0.8)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionSpellStatsReadModel> result = adapter.getChampionSpellStats(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).summoner1Id()).isEqualTo(4);
        assertThat(result.get(0).summoner2Id()).isEqualTo(14);
    }

    @DisplayName("챔피언 스킬 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionSkillBuilds() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionSkillBuildReadModel> expected = List.of(
            new ChampionSkillBuildReadModel("QWEQEEREQEQWWWW", 400, 0.525, 0.4)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionSkillBuildReadModel> result = adapter.getChampionSkillBuilds(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).skillBuild()).isEqualTo("QWEQEEREQEQWWWW");
    }

    @DisplayName("챔피언 시작 아이템 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionStartItemBuilds() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionStartItemBuildReadModel> expected = List.of(
            new ChampionStartItemBuildReadModel(List.of(1056, 2003), 600, 0.51, 0.6)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionStartItemBuildReadModel> result = adapter.getChampionStartItemBuilds(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startItems()).containsExactly(1056, 2003);
    }

    @DisplayName("챔피언 신발 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionBootBuilds() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionBootBuildReadModel> expected = List.of(
            new ChampionBootBuildReadModel(3047, 700, 0.53, 0.7)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionBootBuildReadModel> result =
            adapter.getChampionBootBuilds(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).bootId()).isEqualTo(3047);
        assertThat(result.get(0).winRate()).isEqualTo(0.53);
    }

    @DisplayName("챔피언 3코어 아이템 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionItemBuilds() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<ChampionItemBuildReadModel> expected = List.of(
            new ChampionItemBuildReadModel(List.of(3089, 3157, 3165), 500, 0.52, 0.5)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(expected);

        // when
        List<ChampionItemBuildReadModel> result = adapter.getChampionItemBuilds(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemBuild()).containsExactly(3089, 3157, 3165);
    }

    @DisplayName("포지션별 챔피언 승률/픽률/밴률을 Map으로 반환한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionStatsByPosition() {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        List<AbstractMap.SimpleEntry<String, ChampionRateReadModel>> entries = List.of(
            new AbstractMap.SimpleEntry<>("TOP", new ChampionRateReadModel(266, 0.5200, 0.0800, 0.0500, 1500, null)),
            new AbstractMap.SimpleEntry<>("TOP", new ChampionRateReadModel(122, 0.4800, 0.0600, 0.0300, 1200, null)),
            new AbstractMap.SimpleEntry<>("JUNGLE", new ChampionRateReadModel(64, 0.5100, 0.1000, 0.0700, 2000, null))
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class)))
            .willReturn(entries);

        // when
        Map<String, List<ChampionRateReadModel>> result =
            adapter.getChampionStatsByPosition("16.1", "KR", tierFilter);

        // then
        assertThat(result).containsKeys("TOP", "JUNGLE");
        assertThat(result.get("TOP")).hasSize(2);
        assertThat(result.get("TOP").get(0).championId()).isEqualTo(266);
        assertThat(result.get("TOP").get(0).winRate()).isEqualTo(0.5200);
        assertThat(result.get("JUNGLE")).hasSize(1);
        assertThat(result.get("JUNGLE").get(0).championId()).isEqualTo(64);
    }
}
