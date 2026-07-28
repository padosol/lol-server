package com.example.lolserver.match.adapter.out.persistence.adapter;

import com.example.lolserver.shared.QueueType;
import com.example.lolserver.match.application.model.readmodel.GameReadModel;
import com.example.lolserver.match.application.model.readmodel.GameInfoReadModel;
import com.example.lolserver.match.application.model.readmodel.MSChampionByQueueReadModel;
import com.example.lolserver.match.application.model.readmodel.MSChampionDetailReadModel;
import com.example.lolserver.match.application.model.readmodel.ParticipantReadModel;
import com.example.lolserver.match.application.model.readmodel.ParticipantTimelineReadModel;
import com.example.lolserver.match.application.model.readmodel.TimelineReadModel;
import com.example.lolserver.match.adapter.out.persistence.dto.TimelineEventDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.MSChampionDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.MatchSummonerDTO;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchEntity;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchSummonerEntity;
import com.example.lolserver.match.adapter.out.persistence.mapper.MatchMapper;
import com.example.lolserver.match.adapter.out.persistence.match.MatchRepository;
import com.example.lolserver.match.adapter.out.persistence.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.match.adapter.out.persistence.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.match.adapter.out.persistence.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.match.adapter.out.persistence.timeline.TimelineRepositoryCustom;
import com.example.lolserver.common.support.PaginationRequest;
import com.example.lolserver.common.support.SliceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MatchPersistenceAdapterTest {

    @Mock
    private MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;

    @Mock
    private MatchSummonerRepository matchSummonerRepository;

    @Mock
    private MatchRepositoryCustom matchRepositoryCustom;

    @Mock
    private TimelineRepositoryCustom timelineRepositoryCustom;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchMapper matchMapper;

    private MatchPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MatchPersistenceAdapter(
                matchSummonerRepositoryCustom,
                matchSummonerRepository,
                matchRepositoryCustom,
                timelineRepositoryCustom,
                matchRepository,
                matchMapper,
                Runnable::run
        );
    }

    @DisplayName("PUUID와 queueId로 매치 목록을 조회하면 GameReadModel 페이지를 반환한다")
    @Test
    void getMatches_validParams_returnsGameDataPage() {
        // given
        String puuid = "test-puuid-123";
        Integer queueId = 420;
        PaginationRequest paginationRequest = new PaginationRequest(0, 10, "match", PaginationRequest.SortDirection.DESC);

        MatchSummonerDTO summonerDTO = createMatchSummonerDTO(puuid, "KR_12345");
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12345")
                .queueId(queueId)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .build();

        SliceImpl<MatchEntity> slice = new SliceImpl<>(List.of(matchEntity), PageRequest.of(0, 10), true);

        given(matchRepositoryCustom.getMatches(eq(puuid), eq(queueId), any(Pageable.class))).willReturn(slice);
        given(matchRepositoryCustom.getMatchSummoners("KR_12345")).willReturn(List.of(summonerDTO));
        given(matchMapper.toGameInfoReadModel(any(MatchEntity.class))).willReturn(createGameInfoData(queueId));
        given(matchMapper.toReadModel(any(MatchSummonerDTO.class))).willReturn(createParticipantData(puuid));
        given(timelineRepositoryCustom.selectAllTimelineEventsByMatch(anyString())).willReturn(Collections.emptyList());

        // when
        SliceResult<GameReadModel> result = adapter.getMatches(puuid, queueId, paginationRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        then(matchRepositoryCustom).should().getMatches(eq(puuid), eq(queueId), any(Pageable.class));
    }

    @DisplayName("PUUID와 시즌으로 랭크 챔피언 통계를 솔로/자유로 분리해 조회한다")
    @Test
    void getRankChampions_validParams_returnsMSChampionByQueue() {
        // given
        String puuid = "test-puuid-123";
        Integer season = 14;

        MSChampionDTO soloDto = createMSChampionDTO(
                157, "Yasuo", 10L, 7L, QueueType.RANKED_SOLO_5x5.getQueueId());
        MSChampionDTO flexDto = createMSChampionDTO(
                238, "Zed", 5L, 3L, QueueType.RANKED_FLEX_SR.getQueueId());
        MSChampionDetailReadModel soloDomain = createMSChampion(157, "Yasuo", 10L, 7L);
        MSChampionDetailReadModel flexDomain = createMSChampion(238, "Zed", 5L, 3L);

        given(matchSummonerRepositoryCustom.findAllRankedMatchSummonerByPuuidAndSeason(puuid, season))
                .willReturn(List.of(soloDto, flexDto));
        given(matchMapper.toReadModel(soloDto)).willReturn(soloDomain);
        given(matchMapper.toReadModel(flexDto)).willReturn(flexDomain);

        // when
        MSChampionByQueueReadModel result = adapter.getRankChampions(puuid, season);

        // then
        assertThat(result.solo()).hasSize(1);
        assertThat(result.solo().get(0).getChampionId()).isEqualTo(157);
        assertThat(result.flex()).hasSize(1);
        assertThat(result.flex().get(0).getChampionId()).isEqualTo(238);
        then(matchSummonerRepositoryCustom).should().findAllRankedMatchSummonerByPuuidAndSeason(puuid, season);
    }

    @DisplayName("랭크 챔피언 통계 조회 결과가 비어 있으면 양쪽 모두 빈 리스트를 반환한다")
    @Test
    void getRankChampions_empty_returnsEmptyByQueue() {
        // given
        String puuid = "test-puuid-123";
        Integer season = 14;
        given(matchSummonerRepositoryCustom.findAllRankedMatchSummonerByPuuidAndSeason(puuid, season))
                .willReturn(Collections.emptyList());

        // when
        MSChampionByQueueReadModel result = adapter.getRankChampions(puuid, season);

        // then
        assertThat(result.solo()).isEmpty();
        assertThat(result.flex()).isEmpty();
    }

    @DisplayName("매치 ID로 게임 데이터를 조회하면 Optional<GameReadModel>를 반환한다")
    @Test
    void getGameData_existingMatchId_returnsGameData() {
        // given
        String matchId = "KR_12345";
        MatchSummonerDTO summonerDTO = createMatchSummonerDTO("test-puuid", matchId);

        MatchEntity matchEntity = MatchEntity.builder()
                .matchId(matchId)
                .queueId(420)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .build();

        given(matchRepository.findByMatchId(matchId)).willReturn(Optional.of(matchEntity));
        given(matchRepositoryCustom.getMatchSummoners(matchId)).willReturn(List.of(summonerDTO));
        given(matchMapper.toGameInfoReadModel(any(MatchEntity.class))).willReturn(createGameInfoData(420));
        given(matchMapper.toReadModel(any(MatchSummonerDTO.class))).willReturn(createParticipantData("test-puuid"));
        given(timelineRepositoryCustom.selectAllTimelineEventsByMatch(matchId)).willReturn(Collections.emptyList());

        // when
        Optional<GameReadModel> result = adapter.getGameData(matchId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGameInfoData()).isNotNull();
        then(matchRepository).should().findByMatchId(matchId);
    }

    @DisplayName("존재하지 않는 매치 ID로 조회하면 빈 Optional을 반환한다")
    @Test
    void getGameData_nonExistingMatchId_returnsEmpty() {
        // given
        String matchId = "KR_99999";
        given(matchRepository.findByMatchId(matchId)).willReturn(Optional.empty());

        // when
        Optional<GameReadModel> result = adapter.getGameData(matchId);

        // then
        assertThat(result).isEmpty();
        then(matchRepository).should().findByMatchId(matchId);
    }

    @DisplayName("매치 ID로 타임라인 데이터를 조회한다")
    @Test
    void getTimelineData_validMatchId_returnsTimelineData() {
        // given
        String matchId = "KR_12345";
        TimelineEventDTO itemEvent = new TimelineEventDTO(
                matchId, 1, "ITEM_PURCHASED", 3006, null, null, null, null, null, 60000L);
        TimelineEventDTO skillEvent = new TimelineEventDTO(
                matchId, 1, "SKILL_LEVEL_UP", null, 1, "NORMAL", null, null, null, 30000L);

        given(timelineRepositoryCustom.selectAllTimelineEventsByMatch(matchId))
                .willReturn(List.of(itemEvent, skillEvent));

        // when
        TimelineReadModel result = adapter.getTimelineData(matchId);

        // then
        assertThat(result).isNotNull();
        ParticipantTimelineReadModel timeline = result.participants().get(1);
        assertThat(timeline).isNotNull();
        assertThat(timeline.itemSeq()).hasSize(1);
        assertThat(timeline.itemSeq().get(0).itemId()).isEqualTo(3006);
        assertThat(timeline.itemSeq().get(0).minute()).isEqualTo(1L); // 60000ms → 1분
        assertThat(timeline.skillSeq()).hasSize(1);
        assertThat(timeline.skillSeq().get(0).skillSlot()).isEqualTo(1);
        assertThat(timeline.skillSeq().get(0).minute()).isEqualTo(0L); // 30000ms → 0분
        then(timelineRepositoryCustom).should().selectAllTimelineEventsByMatch(matchId);
    }

    @DisplayName("PUUID로 매치 ID 목록을 페이징하여 조회한다")
    @Test
    void findAllMatchIds_validParams_returnsMatchIdPage() {
        // given
        String puuid = "test-puuid-123";
        Integer queueId = 420;
        PaginationRequest paginationRequest = new PaginationRequest(0, 20, "match", PaginationRequest.SortDirection.DESC);

        List<String> matchIds = List.of("KR_12345", "KR_12346", "KR_12347");
        SliceImpl<String> slice = new SliceImpl<>(matchIds, PageRequest.of(0, 20), false);

        given(matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(eq(puuid), eq(queueId), any(Pageable.class)))
                .willReturn(slice);

        // when
        SliceResult<String> result = adapter.findAllMatchIds(puuid, queueId, paginationRequest);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getContent()).containsExactly("KR_12345", "KR_12346", "KR_12347");
        then(matchSummonerRepositoryCustom).should().findAllMatchIdsByPuuidWithPage(eq(puuid), eq(queueId), any(Pageable.class));
    }

    @DisplayName("아레나 모드(queueId 1700)에서는 placement 순으로 정렬된다")
    @Test
    void getMatches_arenaMode_sortsByPlacement() {
        // given
        String puuid = "test-puuid-123";
        Integer queueId = 1700; // Arena mode
        PaginationRequest paginationRequest = new PaginationRequest(0, 10, "match", PaginationRequest.SortDirection.DESC);

        MatchSummonerDTO summonerDTO1 = createMatchSummonerDTO(puuid, "KR_12345");
        MatchSummonerDTO summonerDTO2 = createMatchSummonerDTO("other-puuid", "KR_12345");

        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12345")
                .queueId(queueId)
                .gameDuration(1200L)
                .gameMode("CHERRY")
                .build();

        SliceImpl<MatchEntity> slice = new SliceImpl<>(List.of(matchEntity), PageRequest.of(0, 10), false);

        given(matchRepositoryCustom.getMatches(eq(puuid), eq(queueId), any(Pageable.class))).willReturn(slice);
        given(matchRepositoryCustom.getMatchSummoners("KR_12345")).willReturn(List.of(summonerDTO1, summonerDTO2));
        given(matchMapper.toGameInfoReadModel(any(MatchEntity.class))).willReturn(createGameInfoData(queueId));

        ParticipantReadModel participant1 = createParticipantDataWithPlacement(puuid, 3);
        ParticipantReadModel participant2 = createParticipantDataWithPlacement("other-puuid", 1);

        given(matchMapper.toReadModel(summonerDTO1)).willReturn(participant1);
        given(matchMapper.toReadModel(summonerDTO2)).willReturn(participant2);
        given(timelineRepositoryCustom.selectAllTimelineEventsByMatch(anyString())).willReturn(Collections.emptyList());

        // when
        SliceResult<GameReadModel> result = adapter.getMatches(puuid, queueId, paginationRequest);

        // then
        assertThat(result.getContent()).hasSize(1);
        GameReadModel gameData = result.getContent().get(0);
        assertThat(gameData.getParticipantData().get(0).getPlacement()).isEqualTo(1);
        assertThat(gameData.getParticipantData().get(1).getPlacement()).isEqualTo(3);
    }

    @DisplayName("매치 ID로 게임 데이터를 조회할 때 팀 정보가 포함된다")
    @Test
    void getGameData_withTeamData_returnsGameDataWithTeamInfo() {
        // given
        String matchId = "KR_12345";

        MatchSummonerDTO blueSummoner = createMatchSummonerDTO("blue-puuid", matchId);
        blueSummoner.setTeamId(100);
        blueSummoner.setWin(true);
        blueSummoner.setTeamChampionKills(25);

        MatchSummonerDTO redSummoner = createMatchSummonerDTO("red-puuid", matchId);
        redSummoner.setTeamId(200);
        redSummoner.setWin(false);
        redSummoner.setTeamChampionKills(15);

        MatchEntity matchEntity = MatchEntity.builder()
                .matchId(matchId)
                .queueId(420)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .build();

        given(matchRepository.findByMatchId(matchId)).willReturn(Optional.of(matchEntity));
        given(matchRepositoryCustom.getMatchSummoners(matchId)).willReturn(List.of(blueSummoner, redSummoner));
        given(matchMapper.toGameInfoReadModel(any(MatchEntity.class))).willReturn(createGameInfoData(420));
        given(matchMapper.toReadModel(any(MatchSummonerDTO.class))).willReturn(createParticipantData("test-puuid"));
        given(timelineRepositoryCustom.selectAllTimelineEventsByMatch(matchId)).willReturn(Collections.emptyList());

        // when
        Optional<GameReadModel> result = adapter.getGameData(matchId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTeamInfoData()).isNotNull();
        assertThat(result.get().getTeamInfoData().getBlueTeam()).isNotNull();
        assertThat(result.get().getTeamInfoData().getBlueTeam().getTeamId()).isEqualTo(100);
        assertThat(result.get().getTeamInfoData().getBlueTeam().isWin()).isTrue();
        assertThat(result.get().getTeamInfoData().getBlueTeam().getChampionKills()).isEqualTo(25);
        assertThat(result.get().getTeamInfoData().getRedTeam()).isNotNull();
        assertThat(result.get().getTeamInfoData().getRedTeam().getTeamId()).isEqualTo(200);
        assertThat(result.get().getTeamInfoData().getRedTeam().isWin()).isFalse();
        assertThat(result.get().getTeamInfoData().getRedTeam().getChampionKills()).isEqualTo(15);
    }

    private MatchSummonerDTO createMatchSummonerDTO(String puuid, String matchId) {
        MatchSummonerDTO dto = new MatchSummonerDTO();
        dto.setPuuid(puuid);
        dto.setMatchId(matchId);
        dto.setParticipantId(1);
        dto.setChampionId(157);
        dto.setChampionName("Yasuo");
        dto.setKills(10);
        dto.setDeaths(5);
        dto.setAssists(8);
        dto.setWin(true);
        dto.setTeamId(100);
        return dto;
    }

    private MatchSummonerEntity createMatchSummonerEntity(String puuid, String matchId) {
        return MatchSummonerEntity.builder()
                .puuid(puuid)
                .matchId(matchId)
                .participantId(1)
                .championId(157)
                .championName("Yasuo")
                .kills(10)
                .deaths(5)
                .assists(8)
                .win(true)
                .teamId(100)
                .build();
    }

    private MatchSummonerEntity createMatchSummonerEntityWithPlacement(String puuid, String matchId, int placement) {
        return MatchSummonerEntity.builder()
                .puuid(puuid)
                .matchId(matchId)
                .participantId(1)
                .championId(157)
                .championName("Yasuo")
                .kills(10)
                .deaths(5)
                .assists(8)
                .win(true)
                .teamId(100)
                .placement(placement)
                .build();
    }

    private GameInfoReadModel createGameInfoData(int queueId) {
        GameInfoReadModel gameInfoData = new GameInfoReadModel();
        gameInfoData.setMatchId("KR_12345");
        gameInfoData.setQueueId(queueId);
        gameInfoData.setGameDuration(1800L);
        gameInfoData.setGameMode("CLASSIC");
        return gameInfoData;
    }

    private ParticipantReadModel createParticipantData(String puuid) {
        ParticipantReadModel data = new ParticipantReadModel();
        data.setPuuid(puuid);
        data.setParticipantId(1);
        data.setChampionId(157);
        data.setChampionName("Yasuo");
        data.setKills(10);
        data.setDeaths(5);
        data.setAssists(8);
        data.setWin(true);
        return data;
    }

    private ParticipantReadModel createParticipantDataWithPlacement(String puuid, int placement) {
        ParticipantReadModel data = createParticipantData(puuid);
        data.setPlacement(placement);
        return data;
    }

    private MSChampionDTO createMSChampionDTO(int championId, String championName, Long playCount, Long win) {
        return createMSChampionDTO(championId, championName, playCount, win, QueueType.RANKED_SOLO_5x5.getQueueId());
    }

    private MSChampionDTO createMSChampionDTO(int championId, String championName, Long playCount, Long win, Integer queueId) {
        return new MSChampionDTO(
                8.0,  // assists
                5.0,  // deaths
                10.0, // kills
                championId,
                championName,
                win,
                playCount - win, // losses
                500.0, // damagePerMinute
                3.6,   // kda
                70.0,  // laneMinionsFirst10Minutes
                25.0,  // damageTakenOnTeamPercentage
                400.0, // goldPerMinute
                playCount,
                queueId
        );
    }

    private MSChampionDetailReadModel createMSChampion(int championId, String championName, Long playCount, Long win) {
        return MSChampionDetailReadModel.builder()
                .assists(8.0)
                .deaths(5.0)
                .kills(10.0)
                .championId(championId)
                .championName(championName)
                .win(win)
                .losses(playCount - win)
                .winRate(70.0)
                .damagePerMinute(500.0)
                .kda(3.6)
                .laneMinionsFirst10Minutes(70.0)
                .damageTakenOnTeamPercentage(25.0)
                .goldPerMinute(400.0)
                .playCount(playCount)
                .build();
    }

}
