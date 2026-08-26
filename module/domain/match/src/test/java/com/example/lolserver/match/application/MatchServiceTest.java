package com.example.lolserver.match.application;

import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.command.MatchCommand;
import com.example.lolserver.match.application.port.out.MatchIdsCachePort;
import com.example.lolserver.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.match.application.port.out.MatchSingleCachePort;
import com.example.lolserver.match.application.model.readmodel.GameReadModel;
import com.example.lolserver.match.application.model.readmodel.GameInfoReadModel;
import com.example.lolserver.match.application.model.readmodel.MSChampionByQueueReadModel;
import com.example.lolserver.match.application.model.readmodel.MSChampionDetailReadModel;
import com.example.lolserver.match.application.model.readmodel.TimelineReadModel;
import com.example.lolserver.common.support.PaginationRequest;
import com.example.lolserver.common.support.SliceResult;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private static final String CURRENT_SEASON_VERSION = "16.16.804.9184";
    private static final String PREVIOUS_SEASON_VERSION = "15.24.700.1000";

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

        List<MSChampionDetailReadModel> solo = List.of(
                MSChampionDetailReadModel.builder().championId(1).championName("Annie")
                        .win(20L).losses(10L).playCount(30L).build()
        );
        List<MSChampionDetailReadModel> flex = List.of(
                MSChampionDetailReadModel.builder().championId(2).championName("Olaf")
                        .win(15L).losses(5L).playCount(20L).build()
        );
        MSChampionByQueueReadModel expected = new MSChampionByQueueReadModel(solo, flex);
        given(matchPersistencePort.getRankChampions("test-puuid", 14)).willReturn(expected);

        // when
        MSChampionByQueueReadModel result = matchService.getRankChampions(command);

        // then
        assertThat(result.solo()).hasSize(1);
        assertThat(result.flex()).hasSize(1);
        assertThat(result.solo().get(0).getChampionName()).isEqualTo("Annie");
        assertThat(result.flex().get(0).getChampionName()).isEqualTo("Olaf");
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
        TimelineReadModel timelineData = new TimelineReadModel(new HashMap<>());
        given(matchPersistencePort.getTimelineData(matchId)).willReturn(timelineData);

        // when
        TimelineReadModel result = matchService.getTimelineData(matchId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.participants()).isEmpty();
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

    @DisplayName("getMatchesBatch 캐시에만 있는 최신 매치를 DB 첫 페이지 위에 얹고 DB 꼬리는 자르지 않는다")
    @Test
    void getMatchesBatch_cacheOnlyMatch_prependedWithoutTruncatingDbPage() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(
                List.of(gameOf("KR_OLD_1", 420, 2_000L), gameOf("KR_OLD_2", 420, 1_000L)), true);
        givenDbFirstPage(null, null, dbResult);

        given(matchIdsCachePort.findIds("test-puuid"))
                .willReturn(Optional.of(List.of("KR_NEW", "KR_OLD_1", "KR_OLD_2")));
        given(matchSingleCachePort.findByIds(List.of("KR_NEW")))
                .willReturn(Map.of("KR_NEW", gameOf("KR_NEW", 420, 3_000L)));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent())
                .extracting(game -> game.getGameInfoData().getMatchId())
                .containsExactly("KR_NEW", "KR_OLD_1", "KR_OLD_2");
        assertThat(result.isHasNext()).isTrue();
    }

    @DisplayName("getMatchesBatch 두 번째 페이지부터는 캐시를 보지 않고 DB 결과를 그대로 반환한다")
    @Test
    void getMatchesBatch_secondPage_returnsDbResultWithoutCache() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(1)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(List.of(gameOf("KR_21", 420, 500L)), false);
        given(matchPersistencePort.getMatchesBatch(
                eq("test-puuid"), isNull(), isNull(), any(PaginationRequest.class)))
                .willReturn(dbResult);

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result).isSameAs(dbResult);
        then(matchIdsCachePort).should(never()).findIds(any());
        then(matchSingleCachePort).should(never()).findByIds(anyCollection());

        ArgumentCaptor<PaginationRequest> captor = ArgumentCaptor.forClass(PaginationRequest.class);
        then(matchPersistencePort).should()
                .getMatchesBatch(eq("test-puuid"), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().page()).isEqualTo(1);
    }

    @DisplayName("getMatchesBatch ZSET 캐시 miss 여도 DB 결과를 그대로 반환한다")
    @Test
    void getMatchesBatch_zsetMiss_returnsDbResult() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .queueId(420)
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(
                List.of(gameOf("KR_A", 420, 2_000L), gameOf("KR_B", 420, 1_000L)), false);
        givenDbFirstPage(null, 420, dbResult);
        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.empty());

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result).isSameAs(dbResult);
        then(matchSingleCachePort).should(never()).findByIds(anyCollection());
    }

    @DisplayName("getMatchesBatch 빈 ZSET 캐시 hit 이면 DB 결과를 그대로 반환한다")
    @Test
    void getMatchesBatch_emptyIds_returnsDbResult() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(List.of(gameOf("KR_A", 420, 1_000L)), false);
        givenDbFirstPage(null, null, dbResult);
        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.of(Collections.emptyList()));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result).isSameAs(dbResult);
        then(matchSingleCachePort).should(never()).findByIds(anyCollection());
    }

    @DisplayName("getMatchesBatch DB 첫 페이지 최하단보다 오래된 캐시 매치는 오버레이하지 않는다")
    @Test
    void getMatchesBatch_olderThanPageFloor_excludedFromOverlay() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(
                List.of(gameOf("KR_A", 420, 3_000L), gameOf("KR_B", 420, 2_000L)), true);
        givenDbFirstPage(null, null, dbResult);

        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.of(List.of("KR_OLDER")));
        given(matchSingleCachePort.findByIds(List.of("KR_OLDER")))
                .willReturn(Map.of("KR_OLDER", gameOf("KR_OLDER", 420, 1_500L)));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result).isSameAs(dbResult);
    }

    @DisplayName("getMatchesBatch 단건 캐시에 없는 후보는 DB 폴백 없이 버린다")
    @Test
    void getMatchesBatch_singleCacheMiss_droppedWithoutDbFallback() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(List.of(gameOf("KR_A", 420, 1_000L)), false);
        givenDbFirstPage(null, null, dbResult);

        given(matchIdsCachePort.findIds("test-puuid")).willReturn(Optional.of(List.of("KR_NEW")));
        given(matchSingleCachePort.findByIds(List.of("KR_NEW"))).willReturn(Collections.emptyMap());

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result).isSameAs(dbResult);
        then(matchPersistencePort).should(never()).findMatchesByIds(anyCollection());
    }

    @DisplayName("getMatchesBatch season 필터는 DB 에 그대로 전달되고 오버레이는 gameVersion major 로 걸러진다")
    @Test
    void getMatchesBatch_seasonFilter_passedToDbAndAppliedToOverlay() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .season(16)
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(List.of(gameOf("KR_A", 420, 1_000L)), true);
        givenDbFirstPage(16, null, dbResult);

        given(matchIdsCachePort.findIds("test-puuid"))
                .willReturn(Optional.of(List.of("KR_S16", "KR_S15")));
        given(matchSingleCachePort.findByIds(List.of("KR_S16", "KR_S15"))).willReturn(Map.of(
                "KR_S16", gameOf("KR_S16", 420, 3_000L, "CLASSIC", CURRENT_SEASON_VERSION),
                "KR_S15", gameOf("KR_S15", 420, 2_000L, "CLASSIC", PREVIOUS_SEASON_VERSION)));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent())
                .extracting(game -> game.getGameInfoData().getMatchId())
                .containsExactly("KR_S16", "KR_A");
        then(matchPersistencePort).should()
                .getMatchesBatch(eq("test-puuid"), eq(16), isNull(), any(PaginationRequest.class));
    }

    @DisplayName("getMatchesBatch queueId 필터는 오버레이 후보에도 적용된다")
    @Test
    void getMatchesBatch_queueIdFilter_appliedToOverlay() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .queueId(420)
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(List.of(gameOf("KR_A", 420, 1_000L)), true);
        givenDbFirstPage(null, 420, dbResult);

        given(matchIdsCachePort.findIds("test-puuid"))
                .willReturn(Optional.of(List.of("KR_SOLO", "KR_FLEX")));
        given(matchSingleCachePort.findByIds(List.of("KR_SOLO", "KR_FLEX"))).willReturn(Map.of(
                "KR_SOLO", gameOf("KR_SOLO", 420, 3_000L),
                "KR_FLEX", gameOf("KR_FLEX", 440, 2_000L)));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent())
                .extracting(game -> game.getGameInfoData().getMatchId())
                .containsExactly("KR_SOLO", "KR_A");
    }

    @DisplayName("getMatchesBatch DB 목록에 없는 게임 모드는 오버레이하지 않는다")
    @Test
    void getMatchesBatch_nonListableGameMode_excludedFromOverlay() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        SliceResult<GameReadModel> dbResult = new SliceResult<>(List.of(gameOf("KR_A", 420, 1_000L)), true);
        givenDbFirstPage(null, null, dbResult);

        given(matchIdsCachePort.findIds("test-puuid"))
                .willReturn(Optional.of(List.of("KR_ARAM", "KR_ARENA")));
        given(matchSingleCachePort.findByIds(List.of("KR_ARAM", "KR_ARENA"))).willReturn(Map.of(
                "KR_ARAM", gameOf("KR_ARAM", 450, 3_000L, "ARAM", CURRENT_SEASON_VERSION),
                "KR_ARENA", gameOf("KR_ARENA", 1700, 2_500L, "CHERRY", CURRENT_SEASON_VERSION)));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent())
                .extracting(game -> game.getGameInfoData().getMatchId())
                .containsExactly("KR_ARENA", "KR_A");
    }

    @DisplayName("getMatchesBatch DB 첫 페이지가 비어 있어도 캐시 매치를 반환한다")
    @Test
    void getMatchesBatch_emptyDbPage_returnsOverlayOnly() {
        // given
        MatchCommand command = MatchCommand.builder()
                .puuid("test-puuid")
                .pageNo(0)
                .build();

        givenDbFirstPage(null, null, new SliceResult<>(Collections.emptyList(), false));

        given(matchIdsCachePort.findIds("test-puuid"))
                .willReturn(Optional.of(List.of("KR_NEW_1", "KR_NEW_2")));
        given(matchSingleCachePort.findByIds(List.of("KR_NEW_1", "KR_NEW_2"))).willReturn(Map.of(
                "KR_NEW_1", gameOf("KR_NEW_1", 420, 2_000L),
                "KR_NEW_2", gameOf("KR_NEW_2", 420, 3_000L)));

        // when
        SliceResult<GameReadModel> result = matchService.getMatchesBatch(command);

        // then
        assertThat(result.getContent())
                .extracting(game -> game.getGameInfoData().getMatchId())
                .containsExactly("KR_NEW_2", "KR_NEW_1");
        assertThat(result.isHasNext()).isFalse();
    }

    private void givenDbFirstPage(Integer season, Integer queueId, SliceResult<GameReadModel> dbResult) {
        given(matchPersistencePort.getMatchesBatch(
                eq("test-puuid"),
                season == null ? isNull() : eq(season),
                queueId == null ? isNull() : eq(queueId),
                any(PaginationRequest.class)))
                .willReturn(dbResult);
    }

    private GameReadModel gameOf(String matchId, int queueId, long gameEndTimestamp) {
        return gameOf(matchId, queueId, gameEndTimestamp, "CLASSIC", CURRENT_SEASON_VERSION);
    }

    private GameReadModel gameOf(
            String matchId, int queueId, long gameEndTimestamp, String gameMode, String gameVersion) {
        GameReadModel game = new GameReadModel();
        GameInfoReadModel info = new GameInfoReadModel();
        info.setMatchId(matchId);
        info.setQueueId(queueId);
        info.setGameCreation(gameEndTimestamp);
        info.setGameEndTimestamp(gameEndTimestamp);
        info.setGameMode(gameMode);
        info.setGameVersion(gameVersion);
        game.setGameInfoData(info);
        return game;
    }
}
