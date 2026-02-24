package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
        List<ChampionWinRateResponse> expected = List.of(
            new ChampionWinRateResponse(13, "MIDDLE", 1000, 520, 0.52)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyString(), anyString()))
            .willReturn(expected);

        // when
        List<ChampionWinRateResponse> result = adapter.getChampionWinRates(13, "16.1", "KR");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).teamPosition()).isEqualTo("MIDDLE");
        assertThat(result.get(0).totalWinRate()).isEqualTo(0.52);
    }

    @DisplayName("챔피언 매치업 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionMatchups() {
        // given
        List<ChampionMatchupResponse> expected = List.of(
            new ChampionMatchupResponse(13, 238, "MIDDLE", 200, 110, 0.55)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyString(), anyString()))
            .willReturn(expected);

        // when
        List<ChampionMatchupResponse> result = adapter.getChampionMatchups(13, "16.1", "KR");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).opponentChampionId()).isEqualTo(238);
    }

    @DisplayName("챔피언 아이템 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionItemBuilds() {
        // given
        List<ChampionItemBuildResponse> expected = List.of(
            new ChampionItemBuildResponse(13, "MIDDLE", "[3089,3157]", 500, 260, 0.52)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyString(), anyString()))
            .willReturn(expected);

        // when
        List<ChampionItemBuildResponse> result = adapter.getChampionItemBuilds(13, "16.1", "KR");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).itemsSorted()).isEqualTo("[3089,3157]");
    }

    @DisplayName("챔피언 룬 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionRuneBuilds() {
        // given
        List<ChampionRuneBuildResponse> expected = List.of(
            new ChampionRuneBuildResponse(13, "MIDDLE", 8100, "[8112,8139]", 8300, "[8304,8345]", 300, 160, 0.5333)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyString(), anyString()))
            .willReturn(expected);

        // when
        List<ChampionRuneBuildResponse> result = adapter.getChampionRuneBuilds(13, "16.1", "KR");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).primaryStyleId()).isEqualTo(8100);
    }

    @DisplayName("챔피언 스킬 빌드 통계를 조회한다")
    @Test
    @SuppressWarnings("unchecked")
    void getChampionSkillBuilds() {
        // given
        List<ChampionSkillBuildResponse> expected = List.of(
            new ChampionSkillBuildResponse(13, "MIDDLE", "QWEQEEREQEQWWWW", 400, 210, 0.525)
        );
        given(clickHouseJdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyString(), anyString()))
            .willReturn(expected);

        // when
        List<ChampionSkillBuildResponse> result = adapter.getChampionSkillBuilds(13, "16.1", "KR");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).skillOrder15()).isEqualTo("QWEQEEREQEQWWWW");
    }
}
