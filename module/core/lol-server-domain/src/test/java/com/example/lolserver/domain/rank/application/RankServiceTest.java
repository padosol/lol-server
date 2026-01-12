package com.example.lolserver.domain.rank.application;

import com.example.lolserver.domain.rank.application.dto.RankResponse;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.application.port.out.RankPersistencePort;
import com.example.lolserver.domain.rank.domain.Rank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @DisplayName("랭크 조회 시 검색 조건에 맞는 랭크 응답 리스트를 반환한다")
    @Test
    void getRanks_유효한검색조건_랭크응답리스트반환() {
        // given
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setPlatform("kr");
        searchDto.setTier("DIAMOND");

        List<Rank> ranks = List.of(
                new Rank("Player1", "KR1", 100, 50, 75, "DIAMOND", 300L, "TOP", List.of("Garen", "Darius")),
                new Rank("Player2", "KR2", 80, 40, 60, "DIAMOND", 250L, "MID", List.of("Ahri", "Zed"))
        );
        given(rankPersistencePort.getRanks(searchDto)).willReturn(ranks);

        // when
        List<RankResponse> result = rankService.getRanks(searchDto);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSummonerName()).isEqualTo("Player1");
        assertThat(result.get(1).getSummonerName()).isEqualTo("Player2");
        then(rankPersistencePort).should().getRanks(searchDto);
    }

    @DisplayName("랭크 조회 결과가 없는 경우 빈 리스트를 반환한다")
    @Test
    void getRanks_결과없음_빈리스트반환() {
        // given
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setPlatform("kr");
        searchDto.setTier("CHALLENGER");

        given(rankPersistencePort.getRanks(searchDto)).willReturn(Collections.emptyList());

        // when
        List<RankResponse> result = rankService.getRanks(searchDto);

        // then
        assertThat(result).isEmpty();
        then(rankPersistencePort).should().getRanks(searchDto);
    }

    @DisplayName("도메인 객체가 DTO로 올바르게 변환된다")
    @Test
    void getRanks_도메인객체_DTO변환확인() {
        // given
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setPlatform("kr");

        Rank rank = new Rank("TestPlayer", "TAG1", 50, 30, 100, "GOLD", 150L, "ADC", List.of("Jinx", "Caitlyn"));
        given(rankPersistencePort.getRanks(searchDto)).willReturn(List.of(rank));

        // when
        List<RankResponse> result = rankService.getRanks(searchDto);

        // then
        assertThat(result).hasSize(1);
        RankResponse response = result.get(0);
        assertThat(response.getSummonerName()).isEqualTo("TestPlayer");
        assertThat(response.getTagLine()).isEqualTo("TAG1");
        assertThat(response.getWin()).isEqualTo(50);
        assertThat(response.getLosses()).isEqualTo(30);
        assertThat(response.getPoint()).isEqualTo(100);
        assertThat(response.getTier()).isEqualTo("GOLD");
        assertThat(response.getSummonerLevel()).isEqualTo(150L);
        assertThat(response.getPosition()).isEqualTo("ADC");
        assertThat(response.getChampions()).hasSize(2);
        assertThat(response.getChampions().get(0).get("championName")).isEqualTo("Jinx");
        then(rankPersistencePort).should().getRanks(searchDto);
    }
}
