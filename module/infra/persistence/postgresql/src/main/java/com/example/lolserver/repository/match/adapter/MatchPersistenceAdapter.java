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
import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.TimelineEventDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.mapper.MatchMapper;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.repository.match.matchteam.MatchTeamRepository;
import com.example.lolserver.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.PaginationRequest;
import com.example.lolserver.support.SliceResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
public class MatchPersistenceAdapter implements MatchPersistencePort {

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchSummonerRepository matchSummonerRepository;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;
    private final MatchTeamRepository matchTeamRepository;
    private final MatchMapper matchMapper;
    private final Executor queryExecutor;

    public MatchPersistenceAdapter(
            MatchSummonerRepositoryCustom matchSummonerRepositoryCustom,
            MatchSummonerRepository matchSummonerRepository,
            MatchRepositoryCustom matchRepositoryCustom,
            TimelineRepositoryCustom timelineRepositoryCustom,
            MatchRepository matchRepository,
            MatchTeamRepository matchTeamRepository,
            MatchMapper matchMapper,
            @Qualifier("queryExecutor") Executor queryExecutor
    ) {
        this.matchSummonerRepositoryCustom = matchSummonerRepositoryCustom;
        this.matchSummonerRepository = matchSummonerRepository;
        this.matchRepositoryCustom = matchRepositoryCustom;
        this.timelineRepositoryCustom = timelineRepositoryCustom;
        this.matchRepository = matchRepository;
        this.matchTeamRepository = matchTeamRepository;
        this.matchMapper = matchMapper;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public SliceResult<GameReadModel> getMatches(String puuid, Integer queueId, PaginationRequest paginationRequest) {
        Pageable pageable = toPageable(paginationRequest);
        Slice<MatchEntity> matchesSlice =
                matchRepositoryCustom.getMatches(puuid, queueId, pageable);

        List<GameReadModel> gameDataList = matchesSlice.getContent().stream()
                .map(matchEntity -> convertToGameData(matchEntity, puuid))
                .toList();

        return new SliceResult<>(gameDataList, matchesSlice.hasNext());
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
        List<TimelineEventDTO> events = timelineRepositoryCustom.selectAllTimelineEventsByMatch(matchId);
        return buildTimelineData(events);
    }

    @Override
    public SliceResult<String> findAllMatchIds(String puuid, Integer queueId, PaginationRequest paginationRequest) {
        Pageable pageable = toPageable(paginationRequest);
        Slice<String> matchIdsSlice = matchSummonerRepositoryCustom
                .findAllMatchIdsByPuuidWithPage(puuid, queueId, pageable);
        return new SliceResult<>(matchIdsSlice.getContent(), matchIdsSlice.hasNext());
    }


    @Override
    public SliceResult<GameReadModel> getMatchesBatch(
            String puuid, Integer season, Integer queueId, PaginationRequest paginationRequest
    ) {
        Pageable pageable = toPageable(paginationRequest);
        Slice<MatchDTO> matchesSlice =
                matchRepositoryCustom.getMatchDTOs(puuid, season, queueId, pageable);
        List<MatchDTO> matchDTOs = matchesSlice.getContent();

        if (matchDTOs.isEmpty()) {
            return new SliceResult<>(Collections.emptyList(), false);
        }

        List<String> matchIds = matchDTOs.stream()
                .map(MatchDTO::getMatchId)
                .toList();

        CompletableFuture<Map<String, List<MatchSummonerDTO>>> summonersFuture =
                CompletableFuture.supplyAsync(() ->
                        matchRepositoryCustom.getMatchSummoners(matchIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        MatchSummonerDTO::getMatchId)),
                        queryExecutor);

        CompletableFuture<Map<String, List<TimelineEventDTO>>> timelineEventsFuture =
                CompletableFuture.supplyAsync(() ->
                        timelineRepositoryCustom
                                .selectTimelineEventsByMatchIds(matchIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        TimelineEventDTO::getMatchId)),
                        queryExecutor);

        Map<String, List<MatchSummonerDTO>> participantsByMatch =
                summonersFuture.join();
        Map<String, List<TimelineEventDTO>> timelineEventsByMatch =
                timelineEventsFuture.join();

        List<GameReadModel> gameDataList = matchDTOs.stream()
                .map(matchDTO -> assembleGameDataFromDTO(
                        matchDTO,
                        participantsByMatch.getOrDefault(
                                matchDTO.getMatchId(),
                                Collections.emptyList()),
                        timelineEventsByMatch.getOrDefault(
                                matchDTO.getMatchId(),
                                Collections.emptyList())
                ))
                .toList();

        return new SliceResult<>(gameDataList, matchesSlice.hasNext());
    }

    private GameReadModel assembleGameDataFromDTO(
            MatchDTO matchDTO,
            List<MatchSummonerDTO> summonerDTOs,
            List<TimelineEventDTO> timelineEventDTOs
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

        TimelineData timelineData = buildTimelineData(timelineEventDTOs);

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

        if (!summonerDTOs.isEmpty()) {
            Map<Integer, List<MatchSummonerDTO>> byTeam = summonerDTOs.stream()
                    .collect(Collectors.groupingBy(MatchSummonerDTO::getTeamId));

            TeamInfoData blueTeam = buildTeamInfoData(byTeam.get(100));
            TeamInfoData redTeam = buildTeamInfoData(byTeam.get(200));

            gameData.setTeamInfoData(TeamData.builder()
                    .blueTeam(blueTeam)
                    .redTeam(redTeam)
                    .build());
        }

        return gameData;
    }

    private TimelineData buildTimelineData(List<TimelineEventDTO> events) {
        List<ItemEvents> itemEvents = new ArrayList<>();
        List<SkillEvents> skillEvents = new ArrayList<>();

        for (TimelineEventDTO event : events) {
            if (TimelineEventDTO.SOURCE_ITEM.equals(event.getEventSource())) {
                itemEvents.add(matchMapper.toItemEventsFromTimelineDTO(event));
            } else if (TimelineEventDTO.SOURCE_SKILL.equals(event.getEventSource())) {
                skillEvents.add(matchMapper.toSkillEventsFromTimelineDTO(event));
            }
        }

        return new TimelineData(itemEvents, skillEvents);
    }

    private TeamInfoData buildTeamInfoData(List<MatchSummonerDTO> teamMembers) {
        if (teamMembers == null || teamMembers.isEmpty()) {
            return null;
        }
        MatchSummonerDTO first = teamMembers.get(0);
        TeamInfoData teamInfo = new TeamInfoData();
        teamInfo.setTeamId(first.getTeamId());
        teamInfo.setWin(first.isWin());
        teamInfo.setChampionKills(first.getTeamChampionKills());
        teamInfo.setBaronKills(first.getTeamBaronKills());
        teamInfo.setDragonKills(first.getTeamDragonKills());
        teamInfo.setTowerKills(first.getTeamTowerKills());
        teamInfo.setInhibitorKills(first.getTeamInhibitorKills());
        teamInfo.setGoldTimeline(sumGoldTimelines(teamMembers));
        teamInfo.setTimestamps(getTimestamps(teamMembers));
        return teamInfo;
    }

    private Integer[] sumGoldTimelines(List<MatchSummonerDTO> teamMembers) {
        Integer[] first = teamMembers.stream()
                .map(MatchSummonerDTO::getGoldTimeline)
                .filter(g -> g != null && g.length > 0)
                .findFirst()
                .orElse(null);
        if (first == null) {
            return null;
        }

        int length = first.length;
        Integer[] sum = new Integer[length];
        for (int i = 0; i < length; i++) {
            sum[i] = 0;
        }

        for (MatchSummonerDTO member : teamMembers) {
            Integer[] gold = member.getGoldTimeline();
            if (gold == null) {
                continue;
            }
            for (int i = 0; i < Math.min(length, gold.length); i++) {
                sum[i] += gold[i] != null ? gold[i] : 0;
            }
        }
        return sum;
    }

    private Integer[] getTimestamps(List<MatchSummonerDTO> teamMembers) {
        return teamMembers.stream()
                .map(MatchSummonerDTO::getTimestamps)
                .filter(t -> t != null && t.length > 0)
                .findFirst()
                .orElse(null);
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

    private Pageable toPageable(PaginationRequest request) {
        Sort.Direction direction = request.direction() == PaginationRequest.SortDirection.ASC
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(request.page(), request.size(), Sort.by(direction, request.sortBy()));
    }

    private GameReadModel convertToGameData(MatchEntity matchEntity, String puuid) {
        GameReadModel gameData = new GameReadModel();

        // GameInfoData
        GameInfoData gameInfoData = matchMapper.toGameInfoData(matchEntity);
        gameData.setGameInfoData(gameInfoData);

        // ParticipantsData
        List<MatchSummonerDTO> summonerDTOs =
                matchRepositoryCustom.getMatchSummoners(matchEntity.getMatchId());
        List<ParticipantData> participantDataList = new ArrayList<>(
                summonerDTOs.stream()
                        .map(matchMapper::toDomain)
                        .toList());
        gameData.setParticipantData(participantDataList);

        // Sorting for specific queue types
        if (gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantDataList.sort(Comparator.comparingInt(ParticipantData::getPlacement));
        }

        // TimelineData
        List<TimelineEventDTO> timelineEvents =
                timelineRepositoryCustom.selectAllTimelineEventsByMatch(
                        matchEntity.getMatchId());
        TimelineData timelineData = buildTimelineData(timelineEvents);
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
        Map<Integer, List<MatchSummonerDTO>> byTeam = summonerDTOs.stream()
                .collect(Collectors.groupingBy(MatchSummonerDTO::getTeamId));

        List<MatchTeamEntity> teamEntities = matchTeamRepository.findByMatchId(matchEntity.getMatchId());
        TeamInfoData blueTeam = null;
        TeamInfoData redTeam = null;

        for (MatchTeamEntity teamEntity : teamEntities) {
            TeamInfoData teamInfo = matchMapper.toDomain(teamEntity);
            List<MatchSummonerDTO> teamMembers = byTeam.get(teamEntity.getTeamId());
            if (teamMembers != null) {
                teamInfo.setGoldTimeline(sumGoldTimelines(teamMembers));
                teamInfo.setTimestamps(getTimestamps(teamMembers));
            }
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

        return gameData;
    }
}
