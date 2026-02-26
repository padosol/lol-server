package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionPositionStatsResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionStatsResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;
import com.example.lolserver.domain.championstats.application.port.out.ChampionStatsQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ChampionStatsServiceTest {

    @Mock
    private ChampionStatsQueryPort championStatsQueryPort;

    private ChampionStatsService championStatsService;

    @BeforeEach
    void setUp() {
        championStatsService = new ChampionStatsService(championStatsQueryPort);
    }

    @DisplayName("포지션별로 그룹화된 챔피언 통계를 반환한다")
    @Test
    void getChampionStats_returnsGroupedByPosition() {
        // given
        int championId = 13;
        String patch = "16.1";
        String platformId = "KR";
        String tier = "EMERALD";

        List<ChampionWinRateResponse> winRates = List.of(
            new ChampionWinRateResponse("MIDDLE", 1000, 520, 0.52),
            new ChampionWinRateResponse("TOP", 200, 90, 0.45)
        );
        Map<String, List<ChampionMatchupResponse>> matchups = Map.of(
            "MIDDLE", List.of(new ChampionMatchupResponse(238, 200, 110, 0.55)),
            "TOP", List.of(new ChampionMatchupResponse(86, 50, 20, 0.40))
        );
        Map<String, List<ChampionItemBuildResponse>> itemBuilds = Map.of(
            "MIDDLE", List.of(new ChampionItemBuildResponse("[3089,3157,3165]", 500, 260, 0.52))
        );
        Map<String, List<ChampionRuneBuildResponse>> runeBuilds = Map.of(
            "MIDDLE", List.of(new ChampionRuneBuildResponse(8100, "[8112,8139,8143,8135]", 8300, "[8304,8345]", 300, 160, 0.5333))
        );
        Map<String, List<ChampionSkillBuildResponse>> skillBuilds = Map.of(
            "MIDDLE", List.of(new ChampionSkillBuildResponse("QWEQEEREQEQWWWW", 400, 210, 0.525))
        );

        given(championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tier))
            .willReturn(winRates);
        given(championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tier))
            .willReturn(matchups);
        given(championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tier))
            .willReturn(itemBuilds);
        given(championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tier))
            .willReturn(runeBuilds);
        given(championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tier))
            .willReturn(skillBuilds);

        // when
        ChampionStatsResponse result = championStatsService.getChampionStats(championId, patch, platformId, tier);

        // then
        assertThat(result.tier()).isEqualTo("EMERALD");
        assertThat(result.stats()).hasSize(2);

        ChampionPositionStatsResponse middle = result.stats().get(0);
        assertThat(middle.teamPosition()).isEqualTo("MIDDLE");
        assertThat(middle.winRate()).isEqualTo(0.52);
        assertThat(middle.totalCount()).isEqualTo(1000);
        assertThat(middle.matchups()).hasSize(1);
        assertThat(middle.matchups().get(0).opponentChampionId()).isEqualTo(238);
        assertThat(middle.itemBuilds()).hasSize(1);
        assertThat(middle.runeBuilds()).hasSize(1);
        assertThat(middle.skillBuilds()).hasSize(1);

        ChampionPositionStatsResponse top = result.stats().get(1);
        assertThat(top.teamPosition()).isEqualTo("TOP");
        assertThat(top.winRate()).isEqualTo(0.45);
        assertThat(top.totalCount()).isEqualTo(200);
        assertThat(top.matchups()).hasSize(1);
        assertThat(top.itemBuilds()).isEmpty();
        assertThat(top.runeBuilds()).isEmpty();
        assertThat(top.skillBuilds()).isEmpty();
    }

    @DisplayName("통계 데이터가 없으면 빈 stats 배열을 반환한다")
    @Test
    void getChampionStats_returnsEmptyStats_whenNoData() {
        // given
        int championId = 999;
        String patch = "16.1";
        String platformId = "KR";
        String tier = "EMERALD";

        given(championStatsQueryPort.getChampionWinRates(championId, patch, platformId, tier))
            .willReturn(List.of());
        given(championStatsQueryPort.getChampionMatchups(championId, patch, platformId, tier))
            .willReturn(Map.of());
        given(championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId, tier))
            .willReturn(Map.of());
        given(championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId, tier))
            .willReturn(Map.of());
        given(championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId, tier))
            .willReturn(Map.of());

        // when
        ChampionStatsResponse result = championStatsService.getChampionStats(championId, patch, platformId, tier);

        // then
        assertThat(result.stats()).isEmpty();
    }
}
