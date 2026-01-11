package com.example.lolserver.domain.match.application;

import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.domain.GameData;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.support.Page;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchPersistencePort matchPersistencePort;

    @InjectMocks
    private MatchService matchService;

    @DisplayName("유효한 커맨드로 매치 조회 시 페이징된 결과를 반환한다")
    @Test
    void getMatches_유효한커맨드_페이징결과반환() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .queueId(420)
                .pageNo(0)
                .region("kr")
                .build();

        List<GameData> games = List.of(new GameData(), new GameData());
        Page<GameData> expected = new Page<>(games, true);

        given(matchPersistencePort.getMatches(eq("test-puuid"), eq(420), any(Pageable.class)))
                .willReturn(expected);

        // when
        Page<GameData> result = matchService.getMatches(command);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isHasNext()).isTrue();
        then(matchPersistencePort).should().getMatches(eq("test-puuid"), eq(420), any(Pageable.class));
    }

    @DisplayName("매치 결과가 없으면 빈 페이지를 반환한다")
    @Test
    void getMatches_결과없음_빈페이지반환() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .queueId(420)
                .pageNo(0)
                .build();

        Page<GameData> emptyPage = new Page<>(Collections.emptyList(), false);
        given(matchPersistencePort.getMatches(eq("test-puuid"), eq(420), any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        Page<GameData> result = matchService.getMatches(command);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        then(matchPersistencePort).should().getMatches(eq("test-puuid"), eq(420), any(Pageable.class));
    }

    @DisplayName("유효한 커맨드로 챔피언 통계 조회 시 챔피언 리스트를 반환한다")
    @Test
    void getRankChampions_유효한커맨드_챔피언리스트반환() {
        // given
        MSChampionCommand command = new MSChampionCommand();
        command.setPuuid("test-puuid");
        command.setSeason(14);

        List<MSChampion> champions = List.of(
                new MSChampion(5.0, 3.0, 10.0, 1, "Annie", 20L, 10L, 66.7, 500.0, 5.0, 80.0, 25.0, 300.0, 30L),
                new MSChampion(6.0, 2.0, 8.0, 2, "Olaf", 15L, 5L, 75.0, 450.0, 7.0, 75.0, 30.0, 280.0, 20L)
        );
        given(matchPersistencePort.getRankChampions("test-puuid", 14)).willReturn(champions);

        // when
        List<MSChampion> result = matchService.getRankChampions(command);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getChampionName()).isEqualTo("Annie");
        assertThat(result.get(1).getChampionName()).isEqualTo("Olaf");
        then(matchPersistencePort).should().getRankChampions("test-puuid", 14);
    }

    @DisplayName("챔피언 통계 결과가 없으면 빈 리스트를 반환한다")
    @Test
    void getRankChampions_결과없음_빈리스트반환() {
        // given
        MSChampionCommand command = new MSChampionCommand();
        command.setPuuid("test-puuid");
        command.setSeason(14);

        given(matchPersistencePort.getRankChampions("test-puuid", 14)).willReturn(Collections.emptyList());

        // when
        List<MSChampion> result = matchService.getRankChampions(command);

        // then
        assertThat(result).isEmpty();
        then(matchPersistencePort).should().getRankChampions("test-puuid", 14);
    }

    @DisplayName("존재하는 매치 ID로 조회 시 게임 데이터를 반환한다")
    @Test
    void getGameData_존재하는매치_게임데이터반환() {
        // given
        String matchId = "KR_1234567890";
        GameData gameData = new GameData();
        given(matchPersistencePort.getGameData(matchId)).willReturn(Optional.of(gameData));

        // when
        GameData result = matchService.getGameData(matchId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(gameData);
        then(matchPersistencePort).should().getGameData(matchId);
    }

    @DisplayName("존재하지 않는 매치 ID로 조회 시 예외가 발생한다")
    @Test
    void getGameData_존재하지않는매치_예외발생() {
        // given
        String matchId = "INVALID_MATCH_ID";
        given(matchPersistencePort.getGameData(matchId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> matchService.getGameData(matchId))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND_MATCH_ID);

        then(matchPersistencePort).should().getGameData(matchId);
    }

    @DisplayName("존재하는 매치 ID로 타임라인 조회 시 타임라인 데이터를 반환한다")
    @Test
    void getTimelineData_존재하는매치_타임라인반환() {
        // given
        String matchId = "KR_1234567890";
        TimelineData timelineData = new TimelineData(new HashMap<>());
        given(matchPersistencePort.getTimelineData(matchId)).willReturn(timelineData);

        // when
        TimelineData result = matchService.getTimelineData(matchId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(timelineData);
        then(matchPersistencePort).should().getTimelineData(matchId);
    }

    @DisplayName("유효한 커맨드로 매치 ID 목록 조회 시 페이징된 결과를 반환한다")
    @Test
    void findAllMatchIds_유효한커맨드_매치ID페이지반환() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .queueId(420)
                .pageNo(0)
                .build();

        List<String> matchIds = List.of("KR_111", "KR_222", "KR_333");
        Page<String> expected = new Page<>(matchIds, true);

        given(matchPersistencePort.findAllMatchIds(eq("test-puuid"), eq(420), any(Pageable.class)))
                .willReturn(expected);

        // when
        Page<String> result = matchService.findAllMatchIds(command);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent()).containsExactly("KR_111", "KR_222", "KR_333");
        assertThat(result.isHasNext()).isTrue();
        then(matchPersistencePort).should().findAllMatchIds(eq("test-puuid"), eq(420), any(Pageable.class));
    }
}
