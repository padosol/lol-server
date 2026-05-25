package com.example.lolserver.domain.match.application;

import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.application.port.out.MatchIdsCachePort;
import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.application.port.out.MatchSingleCachePort;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.MSChampionByQueue;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.domain.gamedata.GameInfoData;
import com.example.lolserver.support.PaginationRequest;
import com.example.lolserver.support.SliceResult;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchPersistencePort matchPersistencePort;

    @Mock
    private MatchIdsCachePort matchIdsCachePort;

    @Mock
    private MatchSingleCachePort matchSingleCachePort;

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
                .platformId("kr")
                .build();

        List<GameReadModel> games = List.of(new GameReadModel(), new GameReadModel());
        SliceResult<GameReadModel> expected = new SliceResult<>(games, true);

        given(matchPersistencePort.getMatches(eq("test-puuid"), eq(420), any(PaginationRequest.class)))
                .willReturn(expected);

        // when
        SliceResult<GameReadModel> result = matchService.getMatches(command);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isHasNext()).isTrue();
        then(matchPersistencePort).should().getMatches(eq("test-puuid"), eq(420), any(PaginationRequest.class));
    }

    @DisplayName("유효한 커맨드로 랭크 챔피언 통계 조회 시 솔로/자유 분리 결과를 반환한다")
    @Test
    void getRankChampions_유효한커맨드_솔로자유분리반환() {
        // given
        MSChampionCommand command = new MSChampionCommand();
        command.setPuuid("test-puuid");
        command.setSeason(14);

        List<MSChampion> solo = List.of(
                new MSChampion(5.0, 3.0, 10.0, 1, "Annie", 20L, 10L, 66.7, 500.0, 5.0, 80.0, 25.0, 300.0, 30L)
        );
        List<MSChampion> flex = List.of(
                new MSChampion(6.0, 2.0, 8.0, 2, "Olaf", 15L, 5L, 75.0, 450.0, 7.0, 75.0, 30.0, 280.0, 20L)
        );
        MSChampionByQueue expected = new MSChampionByQueue(solo, flex);
        given(matchPersistencePort.getRankChampions("test-puuid", 14)).willReturn(expected);

        // when
        MSChampionByQueue result = matchService.getRankChampions(command);

        // then
        assertThat(result.solo()).hasSize(1);
        assertThat(result.flex()).hasSize(1);
    }

    @DisplayName("존재하는 매치 ID로 조회 시 게임 데이터를 반환한다")
    @Test
    void getGameData_존재하는매치_게임데이터반환() {
        // given
        String matchId = "KR_1234567890";
        GameReadModel gameData = new GameReadModel();
        given(matchPersistencePort.getGameData(matchId)).willReturn(Optional.of(gameData));

        // when
        GameReadModel result = matchService.getGameData(matchId);

        // then
        assertThat(result).isEqualTo(gameData);
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
        assertThat(result).isEqualTo(timelineData);
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
        SliceResult<String> expected = new SliceResult<>(matchIds, true);

        given(matchPersistencePort.findAllMatchIds(eq("test-puuid"), eq(420), any(PaginationRequest.class)))
                .willReturn(expected);

        // when
        SliceResult<String> result = matchService.findAllMatchIds(command);

        // then
        assertThat(result.getContent()).containsExactly("KR_111", "KR_222", "KR_333");
        assertThat(result.isHasNext()).isTrue();
    }

    @DisplayName("getMatchesBatch ZSET 캐시 hit + 단건 캐시 all hit 이면 DB 조회를 하지 않는다")
    @Test
    void getMatchesBatch_zsetHitAndSingleAllHit_noDbCall() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        List<String> ids = List.of("KR_1", "KR_2");
        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.of(ids));

        Map<String, GameReadModel> cached = new HashMap<>();
        cached.put("KR_1", gameOf("KR_1", 420, 1000L));
        cached.put("KR_2", gameOf("KR_2", 420, 2000L));
        given(matchSingleCachePort.findByIds(ids)).willReturn(cached);

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.isHasNext()).isFalse();
        then(matchPersistencePort).should(never()).findMatchesByIds(anyCollection());
        then(matchPersistencePort).should(never())
                .getMatchesBatch(any(), any(), any(), any(PaginationRequest.class));
    }

    @DisplayName("getMatchesBatch ZSET 캐시 miss 시 DB 직조회로 떨어지고 캐시를 채우지 않는다")
    @Test
    void getMatchesBatch_zsetMiss_loadsFromDbDirectAndDoesNotWriteCache() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .queueId(420)
                .pageNo(0)
                .build();

        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.empty());

        SliceResult<GameReadModel> dbResult = new SliceResult<>(
                List.of(gameOf("KR_A", 420, 1000L), gameOf("KR_B", 420, 2000L)), false);
        given(matchPersistencePort.getMatchesBatch(
                eq("test-puuid"), isNull(), eq(420), any(PaginationRequest.class)))
                .willReturn(dbResult);

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result).isSameAs(dbResult);
        then(matchSingleCachePort).should(never()).findByIds(anyCollection());
        then(matchPersistencePort).should(never()).findMatchesByIds(anyCollection());
    }

    @DisplayName("getMatchesBatch ZSET hit + 단건 캐시 partial miss 시 누락분만 DB IN 조회로 보강하고 캐시는 채우지 않는다")
    @Test
    void getMatchesBatch_zsetHitAndSinglePartialMiss_loadsMissingNoCacheWrite() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        List<String> ids = List.of("KR_1", "KR_2", "KR_3");
        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.of(ids));

        Map<String, GameReadModel> cached = new HashMap<>();
        cached.put("KR_1", gameOf("KR_1", 420, 1000L));
        given(matchSingleCachePort.findByIds(ids)).willReturn(cached);

        GameReadModel game2 = gameOf("KR_2", 420, 2000L);
        GameReadModel game3 = gameOf("KR_3", 420, 3000L);
        given(matchPersistencePort.findMatchesByIds(List.of("KR_2", "KR_3")))
                .willReturn(List.of(game2, game3));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent()).hasSize(3);
        then(matchPersistencePort).should().findMatchesByIds(List.of("KR_2", "KR_3"));
    }

    @DisplayName("getMatchesBatch queueId 필터는 in-memory 로 동작한다")
    @Test
    void getMatchesBatch_queueIdFilter_appliedInMemory() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .queueId(420)
                .pageNo(0)
                .build();

        List<String> ids = List.of("KR_1", "KR_2", "KR_3");
        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.of(ids));

        Map<String, GameReadModel> cached = new HashMap<>();
        cached.put("KR_1", gameOf("KR_1", 420, 1000L));
        cached.put("KR_2", gameOf("KR_2", 440, 2000L)); // 다른 큐
        cached.put("KR_3", gameOf("KR_3", 420, 3000L));
        given(matchSingleCachePort.findByIds(ids)).willReturn(cached);

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(g -> g.getGameInfoData().getMatchId())
                .containsExactly("KR_1", "KR_3");
    }

    @DisplayName("getMatchesBatch season 필터는 캐시를 우회하고 DB getMatchesBatch 를 직접 호출한다")
    @Test
    void getMatchesBatch_seasonFilter_bypassesCacheAndCallsDb() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .season(14)
                .queueId(420)
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(List.of(new GameReadModel()), true);
        given(matchPersistencePort.getMatchesBatch(
                eq("test-puuid"), eq(14), eq(420), any(PaginationRequest.class)))
                .willReturn(dbResult);

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result).isSameAs(dbResult);
        then(matchIdsCachePort).should(never()).findIds(any());
        then(matchSingleCachePort).should(never()).findByIds(any());
    }

    @DisplayName("getMatchesBatch 빈 ZSET 캐시 hit 이면 빈 SliceResult 를 반환한다")
    @Test
    void getMatchesBatch_emptyIds_returnsEmptySlice() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.of(Collections.emptyList()));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        then(matchSingleCachePort).should(never()).findByIds(any());
    }

    private GameReadModel gameOf(String matchId, int queueId, long gameCreation) {
        GameReadModel game = new GameReadModel();
        GameInfoData info = new GameInfoData();
        info.setMatchId(matchId);
        info.setQueueId(queueId);
        info.setGameCreation(gameCreation);
        game.setGameInfoData(info);
        return game;
    }
}
