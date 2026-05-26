package com.example.lolserver.summoner.application;

import com.example.lolserver.summoner.application.model.LeagueReadModel;
import com.example.lolserver.summoner.application.port.out.LeaguePersistencePort;
import com.example.lolserver.summoner.domain.League;
import com.example.lolserver.summoner.domain.vo.LeagueHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

    @Mock
    private LeaguePersistencePort leaguePersistencePort;

    @InjectMocks
    private LeagueService leagueService;

    @DisplayName("puuid로 리그 조회 시 히스토리가 포함된 리그 리스트를 반환한다")
    @Test
    void getLeaguesBypuuid_데이터존재_히스토리포함반환() {
        // given
        String puuid = "test-puuid-123";

        League league = League.builder()
                .id(1L)
                .leagueId("league-id-1")
                .puuid(puuid)
                .queue("RANKED_SOLO_5x5")
                .wins(100)
                .losses(50)
                .winRate(BigDecimal.valueOf(0.67))
                .tier("DIAMOND")
                .rank("I")
                .leaguePoints(75)
                .build();

        LeagueHistory history = new LeagueHistory(
                1L, puuid, "RANKED_SOLO_5x5", "league-id-1",
                100, 50, "DIAMOND", "I", 75, 2275L,
                false, false, false, true, LocalDateTime.now()
        );

        given(leaguePersistencePort.findAllLeaguesByPuuid(puuid)).willReturn(List.of(league));
        given(leaguePersistencePort.findAllHistoryByLeagueSummonerIds(List.of(1L))).willReturn(List.of(history));

        // when
        List<League> result = leagueService.getLeaguesBypuuid(puuid);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLeagueHistory()).hasSize(1);
        assertThat(result.get(0).getLeagueHistory().get(0).leagueSummonerId()).isEqualTo(1L);
        then(leaguePersistencePort).should().findAllLeaguesByPuuid(puuid);
        then(leaguePersistencePort).should().findAllHistoryByLeagueSummonerIds(List.of(1L));
    }

    @DisplayName("puuid에 해당하는 리그가 없는 경우 빈 리스트를 반환한다")
    @Test
    void getLeaguesBypuuid_데이터없음_빈리스트반환() {
        // given
        String puuid = "non-existent-puuid";

        given(leaguePersistencePort.findAllLeaguesByPuuid(puuid)).willReturn(Collections.emptyList());
        given(leaguePersistencePort.findAllHistoryByLeagueSummonerIds(anyList())).willReturn(Collections.emptyList());

        // when
        List<League> result = leagueService.getLeaguesBypuuid(puuid);

        // then
        assertThat(result).isEmpty();
        then(leaguePersistencePort).should().findAllLeaguesByPuuid(puuid);
    }

    @DisplayName("다중 리그 조회 시 각 리그에 해당하는 히스토리만 정확히 매핑된다")
    @Test
    void getLeaguesBypuuid_다중리그_히스토리정확히매핑() {
        // given
        String puuid = "test-puuid-456";

        League soloLeague = League.builder()
                .id(1L)
                .leagueId("solo-league-id")
                .puuid(puuid)
                .queue("RANKED_SOLO_5x5")
                .tier("DIAMOND")
                .rank("II")
                .build();

        League flexLeague = League.builder()
                .id(2L)
                .leagueId("flex-league-id")
                .puuid(puuid)
                .queue("RANKED_FLEX_SR")
                .tier("PLATINUM")
                .rank("I")
                .build();

        LeagueHistory soloHistory1 = new LeagueHistory(
                1L, puuid, "RANKED_SOLO_5x5", "solo-league-id",
                50, 30, "DIAMOND", "II", 50, 2250L,
                false, false, false, false, LocalDateTime.now().minusDays(1)
        );

        LeagueHistory soloHistory2 = new LeagueHistory(
                1L, puuid, "RANKED_SOLO_5x5", "solo-league-id",
                55, 32, "DIAMOND", "II", 60, 2260L,
                false, false, false, false, LocalDateTime.now()
        );

        LeagueHistory flexHistory = new LeagueHistory(
                2L, puuid, "RANKED_FLEX_SR", "flex-league-id",
                20, 15, "PLATINUM", "I", 40, 2040L,
                false, false, false, false, LocalDateTime.now()
        );

        given(leaguePersistencePort.findAllLeaguesByPuuid(puuid)).willReturn(List.of(soloLeague, flexLeague));
        given(leaguePersistencePort.findAllHistoryByLeagueSummonerIds(List.of(1L, 2L)))
                .willReturn(List.of(soloHistory1, soloHistory2, flexHistory));

        // when
        List<League> result = leagueService.getLeaguesBypuuid(puuid);

        // then
        assertThat(result).hasSize(2);

        League resultSoloLeague = result.stream()
                .filter(l -> l.getQueue().equals("RANKED_SOLO_5x5"))
                .findFirst().orElseThrow();
        assertThat(resultSoloLeague.getLeagueHistory()).hasSize(2);

        League resultFlexLeague = result.stream()
                .filter(l -> l.getQueue().equals("RANKED_FLEX_SR"))
                .findFirst().orElseThrow();
        assertThat(resultFlexLeague.getLeagueHistory()).hasSize(1);

        then(leaguePersistencePort).should().findAllLeaguesByPuuid(puuid);
        then(leaguePersistencePort).should().findAllHistoryByLeagueSummonerIds(List.of(1L, 2L));
    }

    @DisplayName("리그 요약 조회 시 큐/티어/랭크/LP만 담은 ReadModel을 반환한다(히스토리 조회 안 함)")
    @Test
    void getLeagueSummariesByPuuid_요약ReadModel반환() {
        // given
        String puuid = "test-puuid-789";
        League solo = League.builder()
                .id(1L).leagueId("solo").puuid(puuid)
                .queue("RANKED_SOLO_5x5")
                .tier("DIAMOND").rank("I").leaguePoints(75)
                .build();
        given(leaguePersistencePort.findAllLeaguesByPuuid(puuid)).willReturn(List.of(solo));

        // when
        List<LeagueReadModel> result = leagueService.getLeagueSummariesByPuuid(puuid);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQueue()).isEqualTo("RANKED_SOLO_5x5");
        assertThat(result.get(0).getTier()).isEqualTo("DIAMOND");
        assertThat(result.get(0).getRank()).isEqualTo("I");
        assertThat(result.get(0).getLeaguePoints()).isEqualTo(75);
        then(leaguePersistencePort).should().findAllLeaguesByPuuid(puuid);
        then(leaguePersistencePort).should(org.mockito.Mockito.never())
                .findAllHistoryByLeagueSummonerIds(anyList());
    }
}
