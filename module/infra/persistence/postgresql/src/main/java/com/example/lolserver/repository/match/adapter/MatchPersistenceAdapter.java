package com.example.lolserver.repository.match.adapter;

import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.domain.match.application.model.GameReadModel;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.domain.gamedata.GameInfoData;
import com.example.lolserver.domain.match.domain.gamedata.ParticipantData;
import com.example.lolserver.domain.match.domain.gamedata.TeamInfoData;
import com.example.lolserver.domain.match.domain.TeamData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.ParticipantTimeline;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.SkillEvents;
import com.example.lolserver.repository.match.dto.ItemEventDTO;
import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.MatchTeamDTO;
import com.example.lolserver.repository.match.dto.SkillEventDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.mapper.MatchMapper;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.repository.match.matchteam.MatchTeamRepository;
import com.example.lolserver.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MatchPersistenceAdapter implements MatchPersistencePort {

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchSummonerRepository matchSummonerRepository;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchMapper matchMapper;

    @Override
    public Page<GameReadModel> getMatches(String puuid, Integer queueId, Pageable pageable) {
        Slice<MatchEntity> matchesSlice =
                matchRepositoryCustom.getMatches(puuid, queueId, pageable);

        List<GameReadModel> gameDataList = matchesSlice.getContent().stream()
                .map(matchEntity -> convertToGameData(matchEntity, puuid))
                .toList();

        return new Page<>(gameDataList, matchesSlice.hasNext());
    }

    @Override
    public List<MSChampion> getRankChampions(String puuid, Integer season, Integer queueId) {
        return matchSummonerRepositoryCustom.findAllMatchSummonerByPuuidAndSeason(puuid, season, queueId)
                .stream()
                .map(matchMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<GameReadModel> getGameData(String matchId) {
        return matchRepository.findByMatchId(matchId)
                .map(matchEntity -> convertToGameData(matchEntity, null)); // puuid is null if not specific user
    }

    @Override
    public TimelineData getTimelineData(String matchId) {
        List<ItemEventsEntity> persistenceItemEvents = timelineRepositoryCustom.selectAllItemEventsByMatch(matchId);
        List<SkillEventsEntity> persistenceSkillEvents = timelineRepositoryCustom.selectAllSkillEventsByMatch(matchId);

        List<ItemEvents> domainItemEvents = matchMapper.toDomainItemEventsList(persistenceItemEvents);
        List<SkillEvents> domainSkillEvents = matchMapper.toDomainSkillEventsList(persistenceSkillEvents);

        return new TimelineData(domainItemEvents, domainSkillEvents);
    }

    @Override
    public Page<String> findAllMatchIds(String puuid, Integer queueId, Pageable pageable) {
        Slice<String> matchIdsSlice = matchSummonerRepositoryCustom
                .findAllMatchIdsByPuuidWithPage(puuid, queueId, pageable);
        return new Page<>(matchIdsSlice.getContent(), matchIdsSlice.hasNext());
    }


    @Override
    public Page<GameReadModel> getMatchesBatch(
            String puuid, Integer queueId, Pageable pageable
    ) {
        Slice<MatchDTO> matchesSlice =
                matchRepositoryCustom.getMatchDTOs(puuid, queueId, pageable);
        List<MatchDTO> matchDTOs = matchesSlice.getContent();

        if (matchDTOs.isEmpty()) {
            return new Page<>(Collections.emptyList(), false);
        }

        List<String> matchIds = matchDTOs.stream()
                .map(MatchDTO::getMatchId)
                .toList();

        // 배치 쿼리: 병렬 실행으로 네트워크 라운드트립 최소화
        CompletableFuture<Map<String, List<MatchSummonerDTO>>> summonersFuture =
                CompletableFuture.supplyAsync(() ->
                        matchRepositoryCustom.getMatchSummoners(matchIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        MatchSummonerDTO::getMatchId)));

        CompletableFuture<Map<String, List<MatchTeamDTO>>> teamsFuture =
                CompletableFuture.supplyAsync(() ->
                        matchRepositoryCustom.getMatchTeams(matchIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        MatchTeamDTO::getMatchId)));

        CompletableFuture<Map<String, List<ItemEventDTO>>> itemsFuture =
                CompletableFuture.supplyAsync(() ->
                        timelineRepositoryCustom
                                .selectItemEventsByMatchIds(matchIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        ItemEventDTO::getMatchId)));

        CompletableFuture<Map<String, List<SkillEventDTO>>> skillsFuture =
                CompletableFuture.supplyAsync(() ->
                        timelineRepositoryCustom
                                .selectSkillEventsByMatchIds(matchIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        SkillEventDTO::getMatchId)));

        CompletableFuture.allOf(
                summonersFuture, teamsFuture, itemsFuture, skillsFuture
        ).join();

        Map<String, List<MatchSummonerDTO>> participantsByMatch =
                summonersFuture.join();
        Map<String, List<MatchTeamDTO>> teamsByMatch =
                teamsFuture.join();
        Map<String, List<ItemEventDTO>> itemEventsByMatch =
                itemsFuture.join();
        Map<String, List<SkillEventDTO>> skillEventsByMatch =
                skillsFuture.join();

        List<GameReadModel> gameDataList = matchDTOs.stream()
                .map(matchDTO -> assembleGameDataFromDTO(
                        matchDTO,
                        participantsByMatch.getOrDefault(
                                matchDTO.getMatchId(),
                                Collections.emptyList()),
                        teamsByMatch.getOrDefault(
                                matchDTO.getMatchId(),
                                Collections.emptyList()),
                        itemEventsByMatch.getOrDefault(
                                matchDTO.getMatchId(),
                                Collections.emptyList()),
                        skillEventsByMatch.getOrDefault(
                                matchDTO.getMatchId(),
                                Collections.emptyList())
                ))
                .toList();

        return new Page<>(gameDataList, matchesSlice.hasNext());
    }

    private GameReadModel assembleGameDataFromDTO(
            MatchDTO matchDTO,
            List<MatchSummonerDTO> summonerDTOs,
            List<MatchTeamDTO> teamDTOs,
            List<ItemEventDTO> itemEventDTOs,
            List<SkillEventDTO> skillEventDTOs
    ) {
        GameReadModel gameData = new GameReadModel();

        GameInfoData gameInfoData = matchMapper.toGameInfoData(matchDTO);
        gameData.setGameInfoData(gameInfoData);

        List<ParticipantData> participantDataList =
                new ArrayList<>(summonerDTOs.stream()
                        .map(matchMapper::toDomain)
                        .toList());
        gameData.setParticipantData(participantDataList);

        int queueId = gameInfoData.getQueueId();
        if (queueId == 1700 || queueId == 1710) {
            participantDataList.sort(
                    Comparator.comparingInt(ParticipantData::getPlacement));
        }

        List<ItemEvents> domainItemEvents =
                matchMapper.toDomainItemEventDTOList(itemEventDTOs);
        List<SkillEvents> domainSkillEvents =
                matchMapper.toDomainSkillEventDTOList(skillEventDTOs);
        TimelineData timelineData =
                new TimelineData(domainItemEvents, domainSkillEvents);

        for (ParticipantData participant : participantDataList) {
            int participantId = participant.getParticipantId();
            ParticipantTimeline participantTimeline =
                    timelineData.getParticipantTimeline(participantId);
            if (participantTimeline != null) {
                participant.setItemSeq(
                        participantTimeline.getItemSeq());
                participant.setSkillSeq(
                        participantTimeline.getSkillSeq());
            }
        }

        if (!teamDTOs.isEmpty()) {
            TeamInfoData blueTeam = null;
            TeamInfoData redTeam = null;
            for (MatchTeamDTO teamDTO : teamDTOs) {
                TeamInfoData teamInfo = matchMapper.toDomain(teamDTO);
                if (teamDTO.getTeamId() == 100) {
                    blueTeam = teamInfo;
                } else if (teamDTO.getTeamId() == 200) {
                    redTeam = teamInfo;
                }
            }
            gameData.setTeamInfoData(TeamData.builder()
                    .blueTeam(blueTeam)
                    .redTeam(redTeam)
                    .build());
        }

        return gameData;
    }

    @Override
    public List<DailyGameCountReadModel> getDailyGameCounts(
            String puuid, Integer season, Integer queueId, LocalDateTime startDate) {
        return matchSummonerRepositoryCustom
                .findDailyGameCounts(puuid, season, queueId, startDate)
                .stream()
                .map(dto -> new DailyGameCountReadModel(dto.getGameDate(), dto.getGameCount()))
                .toList();
    }

    private GameReadModel convertToGameData(MatchEntity matchEntity, String puuid) {
        GameReadModel gameData = new GameReadModel();

        // GameInfoData
        GameInfoData gameInfoData = matchMapper.toGameInfoData(matchEntity);
        gameData.setGameInfoData(gameInfoData);

        // ParticipantsData
        List<ParticipantData> participantDataList = new ArrayList<>(
                matchSummonerRepository.findByMatchId(matchEntity.getMatchId())
                        .stream()
                        .map(matchMapper::toDomain)
                        .toList());
        gameData.setParticipantData(participantDataList);

        // Sorting for specific queue types
        if (gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantDataList.sort(Comparator.comparingInt(ParticipantData::getPlacement));
        }

        // TimelineData - construct it
        // The previous MatchService had this logic, we need to replicate it here using the domain events
        // Note: The MatchMapper now provides lists of domain events for TimelineData
        List<ItemEventsEntity> persistenceItemEvents =
                timelineRepositoryCustom.selectAllItemEventsByMatch(
                        matchEntity.getMatchId());
        List<SkillEventsEntity> persistenceSkillEvents =
                timelineRepositoryCustom.selectAllSkillEventsByMatch(
                        matchEntity.getMatchId());

        List<ItemEvents> domainItemEvents = matchMapper.toDomainItemEventsList(persistenceItemEvents);
        List<SkillEvents> domainSkillEvents = matchMapper.toDomainSkillEventsList(persistenceSkillEvents);

        TimelineData timelineData = new TimelineData(domainItemEvents, domainSkillEvents);
        // Integrate timeline data into ParticipantData
        for (ParticipantData participant : participantDataList) {
            int participantId = participant.getParticipantId();
            ParticipantTimeline participantTimeline = timelineData.getParticipantTimeline(participantId);

            if (participantTimeline != null) {
                participant.setItemSeq(participantTimeline.getItemSeq());
                participant.setSkillSeq(participantTimeline.getSkillSeq());
            }
        }


        // TeamInfoData
        List<MatchTeamEntity> teamEntities = matchTeamRepository.findByMatchId(matchEntity.getMatchId());
        if (!teamEntities.isEmpty()) {
            TeamInfoData blueTeam = null;
            TeamInfoData redTeam = null;

            for (MatchTeamEntity teamEntity : teamEntities) {
                TeamInfoData teamInfo = matchMapper.toDomain(teamEntity);
                if (teamEntity.getTeamId() == 100) {
                    blueTeam = teamInfo;
                } else if (teamEntity.getTeamId() == 200) {
                    redTeam = teamInfo;
                }
            }

            gameData.setTeamInfoData(TeamData.builder()
                    .blueTeam(blueTeam)
                    .redTeam(redTeam)
                    .build());
        }

        return gameData;
    }
}
