package com.example.lolserver.domain.league.application;

import com.example.lolserver.domain.league.application.port.LeaguePersistencePort;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
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
import static org.mockito.Mockito.never;

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

    @DisplayName("LP 시계열 조회 시 리그별 최신 history 만 포함하고 무제한 배치 쿼리를 쓰지 않는다")
    @Test
    void getLpTimeline_리그별제한조회_무제한쿼리미사용() {
        // given
        String puuid = "timeline-puuid";

        League soloLeague = League.builder()
                .id(1L).puuid(puuid).queue("RANKED_SOLO_5x5").tier("DIAMOND").rank("II").build();
        League flexLeague = League.builder()
                .id(2L).puuid(puuid).queue("RANKED_FLEX_SR").tier("PLATINUM").rank("I").build();

        LeagueHistory soloHistory = new LeagueHistory(
                1L, puuid, "RANKED_SOLO_5x5", "solo-league-id",
                50, 30, "DIAMOND", "II", 60, 2260L,
                false, false, false, false, LocalDateTime.now()
        );
        LeagueHistory flexHistory = new LeagueHistory(
                2L, puuid, "RANKED_FLEX_SR", "flex-league-id",
                20, 15, "PLATINUM", "I", 40, 2040L,
                false, false, false, false, LocalDateTime.now()
        );

        given(leaguePersistencePort.findAllLeaguesByPuuid(puuid))
                .willReturn(List.of(soloLeague, flexLeague));
        given(leaguePersistencePort.findRecentHistoryByLeagueSummonerId(1L))
                .willReturn(List.of(soloHistory));
        given(leaguePersistencePort.findRecentHistoryByLeagueSummonerId(2L))
                .willReturn(List.of(flexHistory));

        // when
        List<League> result = leagueService.getLpTimeline(puuid);

        // then
        assertThat(result).hasSize(2);

        League resultSolo = result.stream()
                .filter(l -> l.getQueue().equals("RANKED_SOLO_5x5"))
                .findFirst().orElseThrow();
        assertThat(resultSolo.getLeagueHistory()).hasSize(1);
        assertThat(resultSolo.getLeagueHistory().get(0).leagueSummonerId()).isEqualTo(1L);

        League resultFlex = result.stream()
                .filter(l -> l.getQueue().equals("RANKED_FLEX_SR"))
                .findFirst().orElseThrow();
        assertThat(resultFlex.getLeagueHistory()).hasSize(1);

        // 리그(큐)별 제한 쿼리가 각각 호출되고, 무제한 배치 쿼리는 호출되지 않아야 한다
        then(leaguePersistencePort).should().findRecentHistoryByLeagueSummonerId(1L);
        then(leaguePersistencePort).should().findRecentHistoryByLeagueSummonerId(2L);
        then(leaguePersistencePort).should(never()).findAllHistoryByLeagueSummonerIds(anyList());
    }
}
