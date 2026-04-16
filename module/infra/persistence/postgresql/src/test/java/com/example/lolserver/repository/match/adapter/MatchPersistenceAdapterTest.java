package com.example.lolserver.repository.match.adapter;

import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.domain.gamedata.GameInfoData;
import com.example.lolserver.domain.match.domain.gamedata.ParticipantData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.SkillEvents;
import com.example.lolserver.repository.match.dto.TimelineEventDTO;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.mapper.MatchMapper;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.PaginationRequest;
import com.example.lolserver.support.SliceResult;
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
        given(matchMapper.toGameInfoData(any(MatchEntity.class))).willReturn(createGameInfoData(queueId));
        given(matchMapper.toDomain(any(MatchSummonerDTO.class))).willReturn(createParticipantData(puuid));
        given(timelineRepositoryCustom.selectAllTimelineEventsByMatch(anyString())).willReturn(Collections.emptyList());

        // when
        SliceResult<GameReadModel> result = adapter.getMatches(puuid, queueId, paginationRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        then(matchRepositoryCustom).should().getMatches(eq(puuid), eq(queueId), any(Pageable.class));
    }

    @DisplayName("PUUID와 시즌, queueId로 랭크 챔피언 통계를 조회한다")
    @Test
    void getRankChampions_validParams_returnsMSChampionList() {
        // given
        String puuid = "test-puuid-123";
        Integer season = 14;
        Integer queueId = 420;

        MSChampionDTO dto = createMSChampionDTO(157, "Yasuo", 10L, 7L);
        MSChampion expected = createMSChampion(157, "Yasuo", 10L, 7L);

        given(matchSummonerRepositoryCustom.findAllMatchSummonerByPuuidAndSeason(puuid, season, queueId))
                .willReturn(List.of(dto));
        given(matchMapper.toDomain(any(MSChampionDTO.class))).willReturn(expected);

        // when
        List<MSChampion> result = adapter.getRankChampions(puuid, season, queueId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChampionId()).isEqualTo(157);
        assertThat(result.get(0).getPlayCount()).isEqualTo(10L);
        assertThat(result.get(0).getWin()).isEqualTo(7L);
        then(matchSummonerRepositoryCustom).should().findAllMatchSummonerByPuuidAndSeason(puuid, season, queueId);
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
        given(matchMapper.toGameInfoData(any(MatchEntity.class))).willReturn(createGameInfoData(420));
        given(matchMapper.toDomain(any(MatchSummonerDTO.class))).willReturn(createParticipantData("test-puuid"));
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
        TimelineEventDTO itemEvent = new TimelineEventDTO(matchId, 1, 3006, "ITEM_PURCHASED", 60000L, "ITEM");
        TimelineEventDTO skillEvent = new TimelineEventDTO(matchId, 1, 1, "NORMAL", 30000L, "SKILL");

        given(timelineRepositoryCustom.selectAllTimelineEventsByMatch(matchId))
                .willReturn(List.of(itemEvent, skillEvent));

        ItemEvents domainItemEvents = ItemEvents.builder()
                .participantId(1).itemId(3006).timestamp(60000L).type("ITEM_PURCHASED").build();
        SkillEvents domainSkillEvents = SkillEvents.builder()
                .participantId(1).skillSlot(1).timestamp(30000L).levelUpType("NORMAL").build();

        given(matchMapper.toItemEventsFromTimelineDTO(itemEvent)).willReturn(domainItemEvents);
        given(matchMapper.toSkillEventsFromTimelineDTO(skillEvent)).willReturn(domainSkillEvents);

        // when
        TimelineData result = adapter.getTimelineData(matchId);

        // then
        assertThat(result).isNotNull();
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
        given(matchMapper.toGameInfoData(any(MatchEntity.class))).willReturn(createGameInfoData(queueId));

        ParticipantData participant1 = createParticipantDataWithPlacement(puuid, 3);
        ParticipantData participant2 = createParticipantDataWithPlacement("other-puuid", 1);

        given(matchMapper.toDomain(summonerDTO1)).willReturn(participant1);
        given(matchMapper.toDomain(summonerDTO2)).willReturn(participant2);
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
        given(matchMapper.toGameInfoData(any(MatchEntity.class))).willReturn(createGameInfoData(420));
        given(matchMapper.toDomain(any(MatchSummonerDTO.class))).willReturn(createParticipantData("test-puuid"));
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

    private GameInfoData createGameInfoData(int queueId) {
        GameInfoData gameInfoData = new GameInfoData();
        gameInfoData.setMatchId("KR_12345");
        gameInfoData.setQueueId(queueId);
        gameInfoData.setGameDuration(1800L);
        gameInfoData.setGameMode("CLASSIC");
        return gameInfoData;
    }

    private ParticipantData createParticipantData(String puuid) {
        ParticipantData data = new ParticipantData();
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

    private ParticipantData createParticipantDataWithPlacement(String puuid, int placement) {
        ParticipantData data = createParticipantData(puuid);
        data.setPlacement(placement);
        return data;
    }

    private MSChampionDTO createMSChampionDTO(int championId, String championName, Long playCount, Long win) {
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
                playCount
        );
    }

    private MSChampion createMSChampion(int championId, String championName, Long playCount, Long win) {
        return new MSChampion(
                8.0,  // assists
                5.0,  // deaths
                10.0, // kills
                championId,
                championName,
                win,
                playCount - win, // losses
                70.0, // winRate
                500.0, // damagePerMinute
                3.6,   // kda
                70.0,  // laneMinionsFirst10Minutes
                25.0,  // damageTakenOnTeamPercentage
                400.0, // goldPerMinute
                playCount
        );
    }

}
