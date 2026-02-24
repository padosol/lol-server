package com.example.lolserver.domain.championstats.application;

import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
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

    @DisplayName("5개의 챔피언 통계를 조합하여 반환한다")
    @Test
    void getChampionStats_returnsCompositeResponse() {
        // given
        int championId = 13;
        String patch = "16.1";
        String platformId = "KR";

        List<ChampionWinRateResponse> winRates = List.of(
            new ChampionWinRateResponse(13, "MIDDLE", 1000, 520, 0.52)
        );
        List<ChampionMatchupResponse> matchups = List.of(
            new ChampionMatchupResponse(13, 238, "MIDDLE", 200, 110, 0.55)
        );
        List<ChampionItemBuildResponse> itemBuilds = List.of(
            new ChampionItemBuildResponse(13, "MIDDLE", "[3089,3157,3165]", 500, 260, 0.52)
        );
        List<ChampionRuneBuildResponse> runeBuilds = List.of(
            new ChampionRuneBuildResponse(13, "MIDDLE", 8100, "[8112,8139,8143,8135]", 8300, "[8304,8345]", 300, 160, 0.5333)
        );
        List<ChampionSkillBuildResponse> skillBuilds = List.of(
            new ChampionSkillBuildResponse(13, "MIDDLE", "QWEQEEREQEQWWWW", 400, 210, 0.525)
        );

        given(championStatsQueryPort.getChampionWinRates(championId, patch, platformId))
            .willReturn(winRates);
        given(championStatsQueryPort.getChampionMatchups(championId, patch, platformId))
            .willReturn(matchups);
        given(championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId))
            .willReturn(itemBuilds);
        given(championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId))
            .willReturn(runeBuilds);
        given(championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId))
            .willReturn(skillBuilds);

        // when
        ChampionStatsResponse result = championStatsService.getChampionStats(championId, patch, platformId);

        // then
        assertThat(result.winRates()).hasSize(1);
        assertThat(result.winRates().get(0).totalWinRate()).isEqualTo(0.52);
        assertThat(result.matchups()).hasSize(1);
        assertThat(result.matchups().get(0).opponentChampionId()).isEqualTo(238);
        assertThat(result.itemBuilds()).hasSize(1);
        assertThat(result.runeBuilds()).hasSize(1);
        assertThat(result.skillBuilds()).hasSize(1);
    }

    @DisplayName("통계 데이터가 없으면 빈 리스트를 포함한 응답을 반환한다")
    @Test
    void getChampionStats_returnsEmptyLists_whenNoData() {
        // given
        int championId = 999;
        String patch = "16.1";
        String platformId = "KR";

        given(championStatsQueryPort.getChampionWinRates(championId, patch, platformId))
            .willReturn(List.of());
        given(championStatsQueryPort.getChampionMatchups(championId, patch, platformId))
            .willReturn(List.of());
        given(championStatsQueryPort.getChampionItemBuilds(championId, patch, platformId))
            .willReturn(List.of());
        given(championStatsQueryPort.getChampionRuneBuilds(championId, patch, platformId))
            .willReturn(List.of());
        given(championStatsQueryPort.getChampionSkillBuilds(championId, patch, platformId))
            .willReturn(List.of());

        // when
        ChampionStatsResponse result = championStatsService.getChampionStats(championId, patch, platformId);

        // then
        assertThat(result.winRates()).isEmpty();
        assertThat(result.matchups()).isEmpty();
        assertThat(result.itemBuilds()).isEmpty();
        assertThat(result.runeBuilds()).isEmpty();
        assertThat(result.skillBuilds()).isEmpty();
    }
}
