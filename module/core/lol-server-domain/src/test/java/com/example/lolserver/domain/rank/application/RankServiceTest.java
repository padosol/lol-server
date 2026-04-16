package com.example.lolserver.domain.rank.application;

import com.example.lolserver.domain.rank.application.model.RankReadModel;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.support.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RankServiceTest {

    @Mock
    private RankPersistencePort rankPersistencePort;

    @InjectMocks
    private RankService rankService;

    @DisplayName("랭크 조회 시 검색 조건에 맞는 랭크 응답 페이지를 반환한다")
    @Test
    void getRanks_유효한검색조건_랭크응답페이지반환() {
        // given
        String platformId = "kr";
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setTier("DIAMOND");

        List<Rank> ranks = List.of(
                Rank.builder()
                        .puuid("puuid-1")
                        .currentRank(1)
                        .rankChange(0)
                        .gameName("Player1")
                        .tagLine("KR1")
                        .wins(100)
                        .losses(50)
                        .winRate(new BigDecimal("66.67"))
                        .tier("DIAMOND")
                        .rank("I")
                        .leaguePoints(75)
                        .champions(List.of("Garen", "Darius"))
                        .build(),
                Rank.builder()
                        .puuid("puuid-2")
                        .currentRank(2)
                        .rankChange(1)
                        .gameName("Player2")
                        .tagLine("KR2")
                        .wins(80)
                        .losses(40)
                        .winRate(new BigDecimal("66.67"))
                        .tier("DIAMOND")
                        .rank("II")
                        .leaguePoints(60)
                        .champions(List.of("Ahri", "Zed"))
                        .build()
        );
        PageResult<Rank> rankPage = new PageResult<>(ranks, 1, 50, 2, 1, true, true);
        given(rankPersistencePort.getRanks(searchDto, platformId)).willReturn(rankPage);

        // when
        PageResult<RankReadModel> result = rankService.getRanks(searchDto, platformId);

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).getGameName()).isEqualTo("Player1");
        assertThat(result.content().get(1).getGameName()).isEqualTo("Player2");
        then(rankPersistencePort).should().getRanks(searchDto, platformId);
    }

    @DisplayName("랭크 조회 결과가 없는 경우 빈 페이지를 반환한다")
    @Test
    void getRanks_결과없음_빈페이지반환() {
        // given
        String platformId = "kr";
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setTier("CHALLENGER");

        PageResult<Rank> emptyPage = new PageResult<>(Collections.emptyList(), 1, 50, 0, 0, true, true);
        given(rankPersistencePort.getRanks(searchDto, platformId)).willReturn(emptyPage);

        // when
        PageResult<RankReadModel> result = rankService.getRanks(searchDto, platformId);

        // then
        assertThat(result.content()).isEmpty();
        then(rankPersistencePort).should().getRanks(searchDto, platformId);
    }

    @DisplayName("도메인 객체가 DTO로 올바르게 변환된다")
    @Test
    void getRanks_도메인객체_DTO변환확인() {
        // given
        String platformId = "kr";
        RankSearchDto searchDto = new RankSearchDto();

        Rank rank = Rank.builder()
                .puuid("puuid-test")
                .currentRank(1)
                .rankChange(2)
                .gameName("TestPlayer")
                .tagLine("TAG1")
                .wins(50)
                .losses(30)
                .winRate(new BigDecimal("62.50"))
                .tier("GOLD")
                .rank("IV")
                .leaguePoints(100)
                .champions(List.of("Jinx", "Caitlyn"))
                .build();
        PageResult<Rank> rankPage = new PageResult<>(List.of(rank), 1, 50, 1, 1, true, true);
        given(rankPersistencePort.getRanks(searchDto, platformId)).willReturn(rankPage);

        // when
        PageResult<RankReadModel> result = rankService.getRanks(searchDto, platformId);

        // then
        assertThat(result.content()).hasSize(1);
        RankReadModel response = result.content().get(0);
        assertThat(response.getPuuid()).isEqualTo("puuid-test");
        assertThat(response.getCurrentRank()).isEqualTo(1);
        assertThat(response.getRankChange()).isEqualTo(2);
        assertThat(response.getGameName()).isEqualTo("TestPlayer");
        assertThat(response.getTagLine()).isEqualTo("TAG1");
        assertThat(response.getWins()).isEqualTo(50);
        assertThat(response.getLosses()).isEqualTo(30);
        assertThat(response.getWinRate()).isEqualTo(new BigDecimal("62.50"));
        assertThat(response.getTier()).isEqualTo("GOLD");
        assertThat(response.getRank()).isEqualTo("IV");
        assertThat(response.getLeaguePoints()).isEqualTo(100);
        assertThat(response.getChampions()).hasSize(2);
        assertThat(response.getChampions().get(0)).isEqualTo("Jinx");
        then(rankPersistencePort).should().getRanks(searchDto, platformId);
    }
}
