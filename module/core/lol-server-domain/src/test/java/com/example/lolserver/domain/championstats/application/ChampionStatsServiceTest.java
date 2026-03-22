package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.TierFilter;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionPositionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionWinRateReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsCachePort;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ChampionStatsServiceTest {

    @Mock
    private ChampionStatsQueryPort championStatsQueryPort;

    @Mock
    private ChampionStatsCachePort championStatsCachePort;

    private ChampionStatsService createService(boolean cacheEnabled) {
        return new ChampionStatsService(championStatsQueryPort, championStatsCachePort, cacheEnabled);
    }

    @Nested
    @DisplayName("캐시 미스 시나리오 (cacheEnabled=true)")
    class CacheMissTests {

        @DisplayName("모든 포지션의 챔피언 상세 통계를 그룹핑하여 반환한다")
        @Test
        void getChampionStats_returnsAllPositionStats() {
            // given
            ChampionStatsService service = createService(true);
            int championId = 13;
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            given(championStatsCachePort.findChampionStats(championId, patch, platformId, "EMERALD"))
                .willReturn(null);

            ChampionWinRateReadModel middleWinRate = new ChampionWinRateReadModel("MIDDLE", 1000, 520, 0.52);
            ChampionWinRateReadModel topWinRate = new ChampionWinRateReadModel("TOP", 200, 110, 0.55);

            given(championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tierFilter))
                .willReturn(List.of(middleWinRate, topWinRate));

            // MIDDLE 포지션 상세 통계
            List<ChampionRuneBuildReadModel> middleRuneBuilds = List.of(
                new ChampionRuneBuildReadModel(8100, 8300, 8112, 8139, 8143, 8135, 8304, 8345, 5002, 5008, 5005, 300, 0.5333, 0.6)
            );
            List<ChampionSpellStatsReadModel> middleSpellStats = List.of(
                new ChampionSpellStatsReadModel(4, 14, 800, 0.52, 0.8)
            );
            List<ChampionSkillBuildReadModel> middleSkillBuilds = List.of(
                new ChampionSkillBuildReadModel("QWEQEEREQEQWWWW", 400, 0.525, 0.4)
            );
            List<ChampionStartItemBuildReadModel> middleStartItemBuilds = List.of(
                new ChampionStartItemBuildReadModel("1056,2003", 600, 0.51, 0.6)
            );
            List<ChampionItemBuildReadModel> middleItemBuilds = List.of(
                new ChampionItemBuildReadModel("3089,3157,3165", 500, 0.52, 0.5)
            );
            List<ChampionItemStatsReadModel> middleItemStats1 = List.of(
                new ChampionItemStatsReadModel(3089, "Rabadon's Deathcap", 400, 0.55, 0.4)
            );
            List<ChampionItemStatsReadModel> middleItemStats2 = List.of(
                new ChampionItemStatsReadModel(3157, "Zhonya's Hourglass", 350, 0.53, 0.35)
            );
            List<ChampionItemStatsReadModel> middleItemStats3 = List.of(
                new ChampionItemStatsReadModel(3165, "Morellonomicon", 200, 0.50, 0.2)
            );

            List<ChampionMatchupReadModel> middleStrongMatchups = List.of(
                new ChampionMatchupReadModel(7, 120, 0.5417, 0.12),
                new ChampionMatchupReadModel(103, 100, 0.5300, 0.10),
                new ChampionMatchupReadModel(4, 80, 0.5250, 0.08)
            );
            List<ChampionMatchupReadModel> middleWeakMatchups = List.of(
                new ChampionMatchupReadModel(238, 150, 0.4667, 0.15),
                new ChampionMatchupReadModel(91, 130, 0.4692, 0.13),
                new ChampionMatchupReadModel(55, 90, 0.4778, 0.09)
            );

            given(championStatsQueryPort.getStrongMatchups(championId, patch, platformId, tierFilter, "MIDDLE"))
                .willReturn(middleStrongMatchups);
            given(championStatsQueryPort.getWeakMatchups(championId, patch, platformId, tierFilter, "MIDDLE"))
                .willReturn(middleWeakMatchups);
            given(championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tierFilter, "MIDDLE"))
                .willReturn(middleRuneBuilds);
            given(championStatsQueryPort.getChampionSpellStats(championId, patch, platformId, tierFilter, "MIDDLE"))
                .willReturn(middleSpellStats);
            given(championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tierFilter, "MIDDLE"))
                .willReturn(middleSkillBuilds);
            given(championStatsQueryPort.getChampionStartItemBuilds(championId, patch, platformId, tierFilter, "MIDDLE"))
                .willReturn(middleStartItemBuilds);
            given(championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tierFilter, "MIDDLE"))
                .willReturn(middleItemBuilds);
            given(championStatsQueryPort.getChampionItemStats(championId, patch, platformId, tierFilter, "MIDDLE", 1))
                .willReturn(middleItemStats1);
            given(championStatsQueryPort.getChampionItemStats(championId, patch, platformId, tierFilter, "MIDDLE", 2))
                .willReturn(middleItemStats2);
            given(championStatsQueryPort.getChampionItemStats(championId, patch, platformId, tierFilter, "MIDDLE", 3))
                .willReturn(middleItemStats3);

            // TOP 포지션 상세 통계
            given(championStatsQueryPort.getStrongMatchups(championId, patch, platformId, tierFilter, "TOP"))
                .willReturn(List.of());
            given(championStatsQueryPort.getWeakMatchups(championId, patch, platformId, tierFilter, "TOP"))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tierFilter, "TOP"))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionSpellStats(championId, patch, platformId, tierFilter, "TOP"))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tierFilter, "TOP"))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionStartItemBuilds(championId, patch, platformId, tierFilter, "TOP"))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tierFilter, "TOP"))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionItemStats(championId, patch, platformId, tierFilter, "TOP", 1))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionItemStats(championId, patch, platformId, tierFilter, "TOP", 2))
                .willReturn(List.of());
            given(championStatsQueryPort.getChampionItemStats(championId, patch, platformId, tierFilter, "TOP", 3))
                .willReturn(List.of());

            // when
            ChampionStatsReadModel result = service.getChampionStats(
                championId, patch, platformId, tierFilter);

            // then
            assertThat(result.tier()).isEqualTo("EMERALD");
            assertThat(result.positions()).hasSize(2);

            ChampionPositionStatsReadModel middleStats = result.positions().get(0);
            assertThat(middleStats.teamPosition()).isEqualTo("MIDDLE");
            assertThat(middleStats.winRate()).isEqualTo(0.52);
            assertThat(middleStats.totalGames()).isEqualTo(1000);
            assertThat(middleStats.strongMatchups()).hasSize(3);
            assertThat(middleStats.strongMatchups().get(0).opponentChampionId()).isEqualTo(7);
            assertThat(middleStats.strongMatchups().get(0).winRate()).isEqualTo(0.5417);
            assertThat(middleStats.weakMatchups()).hasSize(3);
            assertThat(middleStats.weakMatchups().get(0).opponentChampionId()).isEqualTo(238);
            assertThat(middleStats.weakMatchups().get(0).winRate()).isEqualTo(0.4667);
            assertThat(middleStats.runeBuilds()).hasSize(1);
            assertThat(middleStats.spellStats()).hasSize(1);
            assertThat(middleStats.spellStats().get(0).summoner1Id()).isEqualTo(4);
            assertThat(middleStats.skillBuilds()).hasSize(1);
            assertThat(middleStats.startItemBuilds()).hasSize(1);
            assertThat(middleStats.startItemBuilds().get(0).startItems()).isEqualTo("1056,2003");
            assertThat(middleStats.itemBuilds()).hasSize(1);
            assertThat(middleStats.itemStatsByOrder()).hasSize(3);

            ChampionPositionStatsReadModel topStats = result.positions().get(1);
            assertThat(topStats.teamPosition()).isEqualTo("TOP");
            assertThat(topStats.winRate()).isEqualTo(0.55);
            assertThat(topStats.totalGames()).isEqualTo(200);

            then(championStatsCachePort).should().saveChampionStats(
                eq(championId), eq(patch), eq(platformId), eq("EMERALD"), any(ChampionStatsReadModel.class));
        }

        @DisplayName("승률 데이터가 없으면 빈 포지션 리스트를 반환한다")
        @Test
        void getChampionStats_returnsEmptyPositions_whenNoWinRateData() {
            // given
            ChampionStatsService service = createService(true);
            int championId = 999;
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            given(championStatsCachePort.findChampionStats(championId, patch, platformId, "EMERALD"))
                .willReturn(null);

            given(championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tierFilter))
                .willReturn(List.of());

            // when
            ChampionStatsReadModel result = service.getChampionStats(
                championId, patch, platformId, tierFilter);

            // then
            assertThat(result.tier()).isEqualTo("EMERALD");
            assertThat(result.positions()).isEmpty();
        }

        @DisplayName("포지션별 챔피언 승률/픽률/밴률을 반환한다")
        @Test
        void getChampionStatsByPosition_returnsGroupedByPosition() {
            // given
            ChampionStatsService service = createService(true);
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            given(championStatsCachePort.findChampionStatsByPosition(patch, platformId, "EMERALD"))
                .willReturn(null);

            Map<String, List<ChampionRateReadModel>> grouped = Map.of(
                "TOP", List.of(
                    new ChampionRateReadModel(266, 0.5200, 0.0800, 0.0500, 1500),
                    new ChampionRateReadModel(122, 0.4800, 0.0600, 0.0300, 1200)
                ),
                "JUNGLE", List.of(
                    new ChampionRateReadModel(64, 0.5100, 0.1000, 0.0700, 2000)
                )
            );

            given(championStatsQueryPort.getChampionStatsByPosition(patch, platformId, tierFilter))
                .willReturn(grouped);

            // when
            List<PositionChampionStatsReadModel> result =
                service.getChampionStatsByPosition(patch, platformId, tierFilter);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(PositionChampionStatsReadModel::teamPosition)
                .containsExactlyInAnyOrder("TOP", "JUNGLE");

            result.forEach(position ->
                position.champions().forEach(champion ->
                    assertThat(champion.tier()).isNotNull()
                )
            );

            then(championStatsCachePort).should().saveChampionStatsByPosition(
                eq(patch), eq(platformId), eq("EMERALD"), any());
        }

        @DisplayName("포지션별 챔피언 통계 조회 시 데이터가 없으면 빈 리스트를 반환한다")
        @Test
        void getChampionStatsByPosition_returnsEmptyList_whenNoData() {
            // given
            ChampionStatsService service = createService(true);
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            given(championStatsCachePort.findChampionStatsByPosition(patch, platformId, "EMERALD"))
                .willReturn(null);

            given(championStatsQueryPort.getChampionStatsByPosition(patch, platformId, tierFilter))
                .willReturn(Map.of());

            // when
            List<PositionChampionStatsReadModel> result =
                service.getChampionStatsByPosition(patch, platformId, tierFilter);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("캐시 히트 시나리오 (cacheEnabled=true)")
    class CacheHitTests {

        @DisplayName("캐시 히트 시 QueryPort를 호출하지 않는다")
        @Test
        void getChampionStats_cacheHit_skipsQueryPort() {
            // given
            ChampionStatsService service = createService(true);
            int championId = 13;
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            ChampionStatsReadModel cached = new ChampionStatsReadModel("EMERALD", List.of());

            given(championStatsCachePort.findChampionStats(championId, patch, platformId, "EMERALD"))
                .willReturn(cached);

            // when
            ChampionStatsReadModel result = service.getChampionStats(championId, patch, platformId, tierFilter);

            // then
            assertThat(result).isSameAs(cached);
            then(championStatsQueryPort).should(never()).getChampionWinRates(anyInt(), anyString(), anyString(), any());
        }

        @DisplayName("포지션별 캐시 히트 시 QueryPort를 호출하지 않는다")
        @Test
        void getChampionStatsByPosition_cacheHit_skipsQueryPort() {
            // given
            ChampionStatsService service = createService(true);
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            List<PositionChampionStatsReadModel> cached = List.of(
                new PositionChampionStatsReadModel("TOP", List.of())
            );

            given(championStatsCachePort.findChampionStatsByPosition(patch, platformId, "EMERALD"))
                .willReturn(cached);

            // when
            List<PositionChampionStatsReadModel> result = service.getChampionStatsByPosition(patch, platformId, tierFilter);

            // then
            assertThat(result).isSameAs(cached);
            then(championStatsQueryPort).should(never()).getChampionStatsByPosition(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("캐시 OFF 시나리오 (cacheEnabled=false)")
    class CacheDisabledTests {

        @DisplayName("캐시 OFF 시 캐시 포트를 호출하지 않는다")
        @Test
        void getChampionStats_cacheDisabled_skipsCachePort() {
            // given
            ChampionStatsService service = createService(false);
            int championId = 13;
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            given(championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tierFilter))
                .willReturn(List.of());

            // when
            ChampionStatsReadModel result = service.getChampionStats(championId, patch, platformId, tierFilter);

            // then
            assertThat(result.tier()).isEqualTo("EMERALD");
            assertThat(result.positions()).isEmpty();
            then(championStatsCachePort).should(never()).findChampionStats(anyInt(), anyString(), anyString(), anyString());
            then(championStatsCachePort).should(never()).saveChampionStats(anyInt(), anyString(), anyString(), anyString(), any());
        }

        @DisplayName("캐시 OFF 시 포지션별 조회에서도 캐시 포트를 호출하지 않는다")
        @Test
        void getChampionStatsByPosition_cacheDisabled_skipsCachePort() {
            // given
            ChampionStatsService service = createService(false);
            String patch = "16.1";
            String platformId = "KR";
            TierFilter tierFilter = TierFilter.of("EMERALD");

            given(championStatsQueryPort.getChampionStatsByPosition(patch, platformId, tierFilter))
                .willReturn(Map.of());

            // when
            List<PositionChampionStatsReadModel> result = service.getChampionStatsByPosition(patch, platformId, tierFilter);

            // then
            assertThat(result).isEmpty();
            then(championStatsCachePort).should(never()).findChampionStatsByPosition(anyString(), anyString(), anyString());
            then(championStatsCachePort).should(never()).saveChampionStatsByPosition(anyString(), anyString(), anyString(), any());
        }
    }
}
