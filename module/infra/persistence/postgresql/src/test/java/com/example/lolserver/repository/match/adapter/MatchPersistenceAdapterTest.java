package com.example.lolserver.repository.match.adapter;

import com.example.lolserver.domain.match.domain.GameData;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.SkillEvents;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.mapper.MatchMapper;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
                matchMapper
        );
    }

    @DisplayName("PUUID와 queueId로 매치 목록을 조회하면 GameData 페이지를 반환한다")
    @Test
    void getMatches_validParams_returnsGameDataPage() {
        // given
        String puuid = "test-puuid-123";
        Integer queueId = 420;
        Pageable pageable = PageRequest.of(0, 10);

        MatchSummonerEntity summonerEntity = createMatchSummonerEntity(puuid, "KR_12345");
        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12345")
                .queueId(queueId)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .build();

        SliceImpl<MatchEntity> slice = new SliceImpl<>(List.of(matchEntity), pageable, true);

        given(matchRepositoryCustom.getMatches(puuid, queueId, pageable)).willReturn(slice);
        given(matchSummonerRepository.findByMatchId("KR_12345")).willReturn(List.of(summonerEntity));
        given(matchMapper.toGameInfoData(any(MatchEntity.class))).willReturn(createGameInfoData(queueId));
        given(matchMapper.toDomain(any(MatchSummonerEntity.class))).willReturn(createParticipantData(puuid));
        given(timelineRepositoryCustom.selectAllItemEventsByMatch(anyString())).willReturn(Collections.emptyList());
        given(timelineRepositoryCustom.selectAllSkillEventsByMatch(anyString())).willReturn(Collections.emptyList());
        given(matchMapper.toDomainItemEventsList(any())).willReturn(Collections.emptyList());
        given(matchMapper.toDomainSkillEventsList(any())).willReturn(Collections.emptyList());

        // when
        Page<GameData> result = adapter.getMatches(puuid, queueId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        then(matchRepositoryCustom).should().getMatches(puuid, queueId, pageable);
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

    @DisplayName("매치 ID로 게임 데이터를 조회하면 Optional<GameData>를 반환한다")
    @Test
    void getGameData_existingMatchId_returnsGameData() {
        // given
        String matchId = "KR_12345";
        MatchSummonerEntity summonerEntity = createMatchSummonerEntity("test-puuid", matchId);

        MatchEntity matchEntity = MatchEntity.builder()
                .matchId(matchId)
                .queueId(420)
                .gameDuration(1800L)
                .gameMode("CLASSIC")
                .build();

        given(matchRepository.findById(matchId)).willReturn(Optional.of(matchEntity));
        given(matchSummonerRepository.findByMatchId(matchId)).willReturn(List.of(summonerEntity));
        given(matchMapper.toGameInfoData(any(MatchEntity.class))).willReturn(createGameInfoData(420));
        given(matchMapper.toDomain(any(MatchSummonerEntity.class))).willReturn(createParticipantData("test-puuid"));
        given(timelineRepositoryCustom.selectAllItemEventsByMatch(matchId)).willReturn(Collections.emptyList());
        given(timelineRepositoryCustom.selectAllSkillEventsByMatch(matchId)).willReturn(Collections.emptyList());
        given(matchMapper.toDomainItemEventsList(any())).willReturn(Collections.emptyList());
        given(matchMapper.toDomainSkillEventsList(any())).willReturn(Collections.emptyList());

        // when
        Optional<GameData> result = adapter.getGameData(matchId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getGameInfoData()).isNotNull();
        then(matchRepository).should().findById(matchId);
    }

    @DisplayName("존재하지 않는 매치 ID로 조회하면 빈 Optional을 반환한다")
    @Test
    void getGameData_nonExistingMatchId_returnsEmpty() {
        // given
        String matchId = "KR_99999";
        given(matchRepository.findById(matchId)).willReturn(Optional.empty());

        // when
        Optional<GameData> result = adapter.getGameData(matchId);

        // then
        assertThat(result).isEmpty();
        then(matchRepository).should().findById(matchId);
    }

    @DisplayName("매치 ID로 타임라인 데이터를 조회한다")
    @Test
    void getTimelineData_validMatchId_returnsTimelineData() {
        // given
        String matchId = "KR_12345";
        List<ItemEventsEntity> itemEventsEntities = List.of(createItemEventsEntity());
        List<SkillEventsEntity> skillEventsEntities = List.of(createSkillEventsEntity());

        ItemEvents itemEvents = ItemEvents.builder()
                .participantId(1)
                .itemId(3006)
                .timestamp(60000L)
                .type("ITEM_PURCHASED")
                .build();
        SkillEvents skillEvents = SkillEvents.builder()
                .participantId(1)
                .skillSlot(1)
                .timestamp(30000L)
                .type("SKILL_LEVEL_UP")
                .build();

        given(timelineRepositoryCustom.selectAllItemEventsByMatch(matchId)).willReturn(itemEventsEntities);
        given(timelineRepositoryCustom.selectAllSkillEventsByMatch(matchId)).willReturn(skillEventsEntities);
        given(matchMapper.toDomainItemEventsList(itemEventsEntities)).willReturn(List.of(itemEvents));
        given(matchMapper.toDomainSkillEventsList(skillEventsEntities)).willReturn(List.of(skillEvents));

        // when
        TimelineData result = adapter.getTimelineData(matchId);

        // then
        assertThat(result).isNotNull();
        then(timelineRepositoryCustom).should().selectAllItemEventsByMatch(matchId);
        then(timelineRepositoryCustom).should().selectAllSkillEventsByMatch(matchId);
    }

    @DisplayName("PUUID로 매치 ID 목록을 페이징하여 조회한다")
    @Test
    void findAllMatchIds_validParams_returnsMatchIdPage() {
        // given
        String puuid = "test-puuid-123";
        Integer queueId = 420;
        Pageable pageable = PageRequest.of(0, 20);

        List<String> matchIds = List.of("KR_12345", "KR_12346", "KR_12347");
        SliceImpl<String> slice = new SliceImpl<>(matchIds, pageable, false);

        given(matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(puuid, queueId, pageable))
                .willReturn(slice);

        // when
        Page<String> result = adapter.findAllMatchIds(puuid, queueId, pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getContent()).containsExactly("KR_12345", "KR_12346", "KR_12347");
        then(matchSummonerRepositoryCustom).should().findAllMatchIdsByPuuidWithPage(puuid, queueId, pageable);
    }

    @DisplayName("아레나 모드(queueId 1700)에서는 placement 순으로 정렬된다")
    @Test
    void getMatches_arenaMode_sortsByPlacement() {
        // given
        String puuid = "test-puuid-123";
        Integer queueId = 1700; // Arena mode
        Pageable pageable = PageRequest.of(0, 10);

        MatchSummonerEntity summoner1 = createMatchSummonerEntityWithPlacement(puuid, "KR_12345", 3);
        MatchSummonerEntity summoner2 = createMatchSummonerEntityWithPlacement("other-puuid", "KR_12345", 1);

        MatchEntity matchEntity = MatchEntity.builder()
                .matchId("KR_12345")
                .queueId(queueId)
                .gameDuration(1200L)
                .gameMode("CHERRY")
                .build();

        SliceImpl<MatchEntity> slice = new SliceImpl<>(List.of(matchEntity), pageable, false);

        given(matchRepositoryCustom.getMatches(puuid, queueId, pageable)).willReturn(slice);
        given(matchSummonerRepository.findByMatchId("KR_12345")).willReturn(List.of(summoner1, summoner2));
        given(matchMapper.toGameInfoData(any(MatchEntity.class))).willReturn(createGameInfoData(queueId));

        ParticipantData participant1 = createParticipantDataWithPlacement(puuid, 3);
        ParticipantData participant2 = createParticipantDataWithPlacement("other-puuid", 1);

        given(matchMapper.toDomain(summoner1)).willReturn(participant1);
        given(matchMapper.toDomain(summoner2)).willReturn(participant2);
        given(timelineRepositoryCustom.selectAllItemEventsByMatch(anyString())).willReturn(Collections.emptyList());
        given(timelineRepositoryCustom.selectAllSkillEventsByMatch(anyString())).willReturn(Collections.emptyList());
        given(matchMapper.toDomainItemEventsList(any())).willReturn(Collections.emptyList());
        given(matchMapper.toDomainSkillEventsList(any())).willReturn(Collections.emptyList());

        // when
        Page<GameData> result = adapter.getMatches(puuid, queueId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        GameData gameData = result.getContent().get(0);
        assertThat(gameData.getParticipantData().get(0).getPlacement()).isEqualTo(1);
        assertThat(gameData.getParticipantData().get(1).getPlacement()).isEqualTo(3);
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

    private ItemEventsEntity createItemEventsEntity() {
        ItemEventsEntity entity = new ItemEventsEntity();
        entity.setParticipantId(1);
        entity.setItemId(3006);
        entity.setTimestamp(60000L);
        entity.setType("ITEM_PURCHASED");
        return entity;
    }

    private SkillEventsEntity createSkillEventsEntity() {
        SkillEventsEntity entity = new SkillEventsEntity();
        entity.setParticipantId(1);
        entity.setSkillSlot(1);
        entity.setTimestamp(30000L);
        entity.setType("SKILL_LEVEL_UP");
        return entity;
    }
}
