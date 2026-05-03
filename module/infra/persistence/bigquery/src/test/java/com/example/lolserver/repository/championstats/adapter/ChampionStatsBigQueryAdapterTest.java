package com.example.lolserver.repository.championstats.adapter;

import com.example.lolserver.TierFilter;
import com.example.lolserver.config.BigQueryProperties;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChampionStatsBigQueryAdapterTest {

    @Mock
    private BigQuery bigQuery;

    private ChampionStatsBigQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        BigQueryProperties properties = new BigQueryProperties("test-project", "lol_analytics", null);
        adapter = new ChampionStatsBigQueryAdapter(bigQuery, properties);
    }

    @DisplayName("챔피언 승률 조회 시 named parameter를 BigQuery 스키마(patch_version_int, tier_bucket)에 맞춰 바인딩한다")
    @Test
    void getChampionWinRates() throws InterruptedException {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD+");
        FieldValueList row = mock(FieldValueList.class);
        given(row.get("team_position")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "MIDDLE"));
        given(row.get("total_games")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1000"));
        given(row.get("total_wins")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "520"));
        given(row.get("total_win_rate")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "0.52"));

        TableResult result = mock(TableResult.class);
        given(result.iterateAll()).willReturn(List.of(row));
        given(bigQuery.query(any(QueryJobConfiguration.class))).willReturn(result);

        // when
        List<ChampionWinRateReadModel> rates =
                adapter.getChampionWinRates(13, "16.1", "KR", tierFilter);

        // then
        assertThat(rates).hasSize(1);
        assertThat(rates.get(0).teamPosition()).isEqualTo("MIDDLE");
        assertThat(rates.get(0).totalGames()).isEqualTo(1000);
        assertThat(rates.get(0).totalWins()).isEqualTo(520);
        assertThat(rates.get(0).totalWinRate()).isEqualTo(0.52);

        ArgumentCaptor<QueryJobConfiguration> captor = ArgumentCaptor.forClass(QueryJobConfiguration.class);
        verify(bigQuery).query(captor.capture());
        Map<String, QueryParameterValue> params = captor.getValue().getNamedParameters();
        assertThat(params).containsKeys("championId", "patch", "platform", "tierBuckets");
        assertThat(params.get("championId").getValue()).isEqualTo("13");
        assertThat(params.get("patch").getValue()).isEqualTo("1601");
        assertThat(params.get("platform").getValue()).isEqualTo("KR");
        assertThat(params.get("tierBuckets").getArrayValues())
                .extracting(QueryParameterValue::getValue)
                .contains("6000", "7000", "8000", "9000", "10000");
        assertThat(captor.getValue().getQuery()).contains("`lol_analytics.mv_champion_pick_stats`");
    }

    @DisplayName("포지션별 챔피언 통계를 Map으로 그룹핑하고 BQ가 산정한 tier 라벨을 그대로 전달한다")
    @Test
    void getChampionStatsByPosition() throws InterruptedException {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD+");
        FieldValueList topRow = stubRateRow("TOP", "266", "0.52", "0.08", "0.05", "1500", "S+");
        FieldValueList topRow2 = stubRateRow("TOP", "122", "0.48", "0.06", "0.03", "1200", "B");
        FieldValueList jungleRow = stubRateRow("JUNGLE", "64", "0.51", "0.10", "0.07", "2000", "A");

        TableResult result = mock(TableResult.class);
        given(result.iterateAll()).willReturn(List.of(topRow, topRow2, jungleRow));
        given(bigQuery.query(any(QueryJobConfiguration.class))).willReturn(result);

        // when
        Map<String, List<ChampionRateReadModel>> grouped =
                adapter.getChampionStatsByPosition("16.1", "KR", tierFilter);

        // then
        assertThat(grouped).containsKeys("TOP", "JUNGLE");
        assertThat(grouped.get("TOP")).hasSize(2);
        assertThat(grouped.get("TOP").get(0).championId()).isEqualTo(266);
        assertThat(grouped.get("TOP").get(0).winRate()).isEqualTo(0.52);
        assertThat(grouped.get("TOP").get(0).tier()).isEqualTo("S+");
        assertThat(grouped.get("TOP").get(1).tier()).isEqualTo("B");
        assertThat(grouped.get("JUNGLE")).hasSize(1);
        assertThat(grouped.get("JUNGLE").get(0).championId()).isEqualTo(64);
        assertThat(grouped.get("JUNGLE").get(0).tier()).isEqualTo("A");
    }

    @DisplayName("단일 티어 필터는 해당 티어의 tier_bucket 값 하나만 바인딩한다")
    @Test
    void singleTierFilterBindsSingleBucket() throws InterruptedException {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD");
        TableResult result = mock(TableResult.class);
        given(result.iterateAll()).willReturn(List.of());
        given(bigQuery.query(any(QueryJobConfiguration.class))).willReturn(result);

        // when
        adapter.getChampionWinRates(13, "16.1", "KR", tierFilter);

        // then
        ArgumentCaptor<QueryJobConfiguration> captor = ArgumentCaptor.forClass(QueryJobConfiguration.class);
        verify(bigQuery).query(captor.capture());
        Map<String, QueryParameterValue> params = captor.getValue().getNamedParameters();
        assertThat(params.get("tierBuckets").getArrayValues())
                .extracting(QueryParameterValue::getValue)
                .containsExactly("6000");
    }

    @DisplayName("매치업 조회 시 rank_type(TOP/BOTTOM)을 그대로 모델에 매핑하고 single UNION 쿼리로 묶는다")
    @Test
    void getChampionMatchupsMapsRankType() throws InterruptedException {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD+");
        FieldValueList topRow = stubMatchupRow("TOP", "266", "120", "0.55", "0.12");
        FieldValueList bottomRow = stubMatchupRow("BOTTOM", "122", "150", "0.45", "0.15");

        TableResult result = mock(TableResult.class);
        given(result.iterateAll()).willReturn(List.of(topRow, bottomRow));
        given(bigQuery.query(any(QueryJobConfiguration.class))).willReturn(result);

        // when
        List<ChampionMatchupReadModel> matchups =
                adapter.getChampionMatchups(13, "16.1", "KR", tierFilter, "MIDDLE");

        // then
        assertThat(matchups).hasSize(2);
        assertThat(matchups.get(0).rankType()).isEqualTo("TOP");
        assertThat(matchups.get(0).opponentChampionId()).isEqualTo(266);
        assertThat(matchups.get(1).rankType()).isEqualTo("BOTTOM");
        assertThat(matchups.get(1).opponentChampionId()).isEqualTo(122);

        ArgumentCaptor<QueryJobConfiguration> captor = ArgumentCaptor.forClass(QueryJobConfiguration.class);
        verify(bigQuery).query(captor.capture());
        assertThat(captor.getValue().getQuery()).contains("UNION ALL");
        assertThat(captor.getValue().getNamedParameters()).containsKeys("championId", "position");
    }

    @DisplayName("BigQuery 쿼리가 인터럽트되면 IllegalStateException을 던지고 인터럽트 플래그를 복원한다")
    @Test
    void queryInterrupted() throws InterruptedException {
        // given
        TierFilter tierFilter = TierFilter.of("EMERALD+");
        given(bigQuery.query(any(QueryJobConfiguration.class)))
                .willThrow(new InterruptedException("test interrupt"));

        // when / then
        try {
            adapter.getChampionWinRates(13, "16.1", "KR", tierFilter);
            org.junit.jupiter.api.Assertions.fail("expected IllegalStateException");
        } catch (IllegalStateException ignored) {
            assertThat(Thread.interrupted()).isTrue();
        }
    }

    private FieldValueList stubMatchupRow(
            String rankType, String opponentChampionId, String games, String winRate, String pickRate) {
        FieldValueList row = mock(FieldValueList.class);
        given(row.get("rank_type")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, rankType));
        given(row.get("opponent_champion_id")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, opponentChampionId));
        given(row.get("games")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, games));
        given(row.get("win_rate")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, winRate));
        given(row.get("pick_rate")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, pickRate));
        return row;
    }

    private FieldValueList stubRateRow(
            String position, String championId, String winRate,
            String pickRate, String banRate, String totalGames, String tier) {
        FieldValueList row = mock(FieldValueList.class);
        given(row.get("team_position")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, position));
        given(row.get("champion_id")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, championId));
        given(row.get("win_rate")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, winRate));
        given(row.get("pick_rate")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, pickRate));
        given(row.get("ban_rate")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, banRate));
        given(row.get("total_games")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, totalGames));
        given(row.get("tier")).willReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, tier));
        return row;
    }
}
