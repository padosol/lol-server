package com.example.lolserver.match.adapter.out.persistence.adapter;

import com.example.lolserver.shared.QueueType;
import com.example.lolserver.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.match.application.model.DailyGameCountReadModel;
import com.example.lolserver.match.application.model.GameInfoReadModel;
import com.example.lolserver.match.application.model.GameReadModel;
import com.example.lolserver.match.application.model.ItemSeqReadModel;
import com.example.lolserver.match.application.model.MSChampionByQueueReadModel;
import com.example.lolserver.match.application.model.MSChampionDetailReadModel;
import com.example.lolserver.match.application.model.ParticipantReadModel;
import com.example.lolserver.match.application.model.ParticipantTimelineReadModel;
import com.example.lolserver.match.application.model.SkillSeqReadModel;
import com.example.lolserver.match.application.model.TeamInfoReadModel;
import com.example.lolserver.match.application.model.TeamReadModel;
import com.example.lolserver.match.application.model.TimelineReadModel;
import com.example.lolserver.match.adapter.out.persistence.dto.MatchDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.MatchSummonerDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.TimelineEventDTO;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchEntity;
import com.example.lolserver.match.adapter.out.persistence.mapper.MatchMapper;
import com.example.lolserver.match.adapter.out.persistence.match.MatchRepository;
import com.example.lolserver.match.adapter.out.persistence.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.match.adapter.out.persistence.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.match.adapter.out.persistence.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.match.adapter.out.persistence.timeline.TimelineRepositoryCustom;
import com.example.lolserver.common.support.PaginationRequest;
import com.example.lolserver.common.support.SliceResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
public class MatchPersistenceAdapter implements MatchPersistencePort {

    private static final String TYPE_ITEM_PURCHASED = "ITEM_PURCHASED";

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchSummonerRepository matchSummonerRepository;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final Executor queryExecutor;

    public MatchPersistenceAdapter(
            MatchSummonerRepositoryCustom matchSummonerRepositoryCustom,
            MatchSummonerRepository matchSummonerRepository,
            MatchRepositoryCustom matchRepositoryCustom,
            TimelineRepositoryCustom timelineRepositoryCustom,
            MatchRepository matchRepository,
            MatchMapper matchMapper,
            @Qualifier("queryExecutor") Executor queryExecutor
    ) {
        this.matchSummonerRepositoryCustom = matchSummonerRepositoryCustom;
        this.matchSummonerRepository = matchSummonerRepository;
        this.matchRepositoryCustom = matchRepositoryCustom;
        this.timelineRepositoryCustom = timelineRepositoryCustom;
        this.matchRepository = matchRepository;
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
    public MSChampionByQueueReadModel getRankChampions(String puuid, Integer season) {
        Map<Integer, List<MSChampionDetailReadModel>> byQueue = matchSummonerRepositoryCustom
                .findAllRankedMatchSummonerByPuuidAndSeason(puuid, season)
                .stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getQueueId() == null ? 0 : dto.getQueueId(),
                        Collectors.mapping(dto -> matchMapper.toReadModel(dto), Collectors.toList())));

        List<MSChampionDetailReadModel> solo = byQueue.getOrDefault(
                QueueType.RANKED_SOLO_5x5.getQueueId(), Collections.emptyList());
        List<MSChampionDetailReadModel> flex = byQueue.getOrDefault(
                QueueType.RANKED_FLEX_SR.getQueueId(), Collections.emptyList());

        return new MSChampionByQueueReadModel(solo, flex);
    }

    @Override
    public Optional<GameReadModel> getGameData(String matchId) {
        return matchRepository.findByMatchId(matchId)
                .map(matchEntity -> convertToGameData(matchEntity, null)); // puuid is null if not specific user
    }

    @Override
    public TimelineReadModel getTimelineData(String matchId) {
        List<TimelineEventDTO> events = timelineRepositoryCustom.selectAllTimelineEventsByMatch(matchId);
        return new TimelineReadModel(buildParticipantTimelines(events));
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

        GameInfoReadModel gameInfoData = matchMapper.toGameInfoReadModel(matchDTO);
        gameData.setGameInfoData(gameInfoData);

        List<ParticipantReadModel> participantDataList =
                new ArrayList<>(summonerDTOs.stream()
                        .map(dto -> matchMapper.toReadModel(dto))
                        .toList());
        gameData.setParticipantData(participantDataList);

        int queueId = gameInfoData.getQueueId();
        if (queueId == 1700 || queueId == 1710) {
            participantDataList.sort(
                    Comparator.comparingInt(ParticipantReadModel::getPlacement));
        }

        applyParticipantTimelines(participantDataList, timelineEventDTOs);

        if (!summonerDTOs.isEmpty()) {
            Map<Integer, List<MatchSummonerDTO>> byTeam = summonerDTOs.stream()
                    .collect(Collectors.groupingBy(MatchSummonerDTO::getTeamId));

            TeamInfoReadModel blueTeam = buildTeamInfoData(byTeam.get(100));
            TeamInfoReadModel redTeam = buildTeamInfoData(byTeam.get(200));

            gameData.setTeamInfoData(TeamReadModel.builder()
                    .blueTeam(blueTeam)
                    .redTeam(redTeam)
                    .build());
        }

        return gameData;
    }

    /**
     * 참가자별 타임라인(아이템/스킬 시퀀스)을 조립해 각 ParticipantReadModel 에 채운다.
     */
    private void applyParticipantTimelines(
            List<ParticipantReadModel> participants, List<TimelineEventDTO> timelineEventDTOs) {
        Map<Integer, ParticipantTimelineReadModel> timelines =
                buildParticipantTimelines(timelineEventDTOs);
        for (ParticipantReadModel participant : participants) {
            ParticipantTimelineReadModel timeline = timelines.get(participant.getParticipantId());
            if (timeline != null) {
                participant.setItemSeq(timeline.itemSeq());
                participant.setSkillSeq(timeline.skillSeq());
            }
        }
    }

    /**
     * 타임라인 이벤트 DTO 를 참가자별 아이템(ITEM_PURCHASED) / 스킬(SKILL_LEVEL_UP) 시퀀스로 정형화한다.
     * minute 은 timestamp(ms) → 분 변환.
     */
    private Map<Integer, ParticipantTimelineReadModel> buildParticipantTimelines(
            List<TimelineEventDTO> events) {
        Map<Integer, List<ItemSeqReadModel>> itemsByParticipant = new LinkedHashMap<>();
        Map<Integer, List<SkillSeqReadModel>> skillsByParticipant = new LinkedHashMap<>();

        for (TimelineEventDTO event : events) {
            int participantId = event.getParticipantId();
            if (TYPE_ITEM_PURCHASED.equalsIgnoreCase(event.getType())) {
                itemsByParticipant.computeIfAbsent(participantId, k -> new ArrayList<>())
                        .add(new ItemSeqReadModel(
                                orZero(event.getItemId()), toMinute(event.getTimestamp()), event.getType()));
            } else if (event.isSkillEvent()) {
                skillsByParticipant.computeIfAbsent(participantId, k -> new ArrayList<>())
                        .add(new SkillSeqReadModel(
                                orZero(event.getSkillSlot()), toMinute(event.getTimestamp()), event.getType()));
            }
        }

        Set<Integer> participantIds = new LinkedHashSet<>();
        participantIds.addAll(itemsByParticipant.keySet());
        participantIds.addAll(skillsByParticipant.keySet());

        Map<Integer, ParticipantTimelineReadModel> result = new LinkedHashMap<>();
        for (Integer participantId : participantIds) {
            result.put(participantId, new ParticipantTimelineReadModel(
                    itemsByParticipant.getOrDefault(participantId, new ArrayList<>()),
                    skillsByParticipant.getOrDefault(participantId, new ArrayList<>())));
        }
        return result;
    }

    private TeamInfoReadModel buildTeamInfoData(List<MatchSummonerDTO> teamMembers) {
        if (teamMembers == null || teamMembers.isEmpty()) {
            return null;
        }
        MatchSummonerDTO first = teamMembers.get(0);
        TeamInfoReadModel teamInfo = new TeamInfoReadModel();
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
    public List<String> findRecentMatchIds(String puuid, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return matchSummonerRepositoryCustom
                .findAllMatchIdsByPuuidWithPage(puuid, null, pageable)
                .getContent();
    }

    @Override
    public List<GameReadModel> findMatchesByIds(Collection<String> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<MatchDTO> matchDTOs = matchRepositoryCustom.getMatchDTOsByIds(matchIds);
        if (matchDTOs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> foundIds = matchDTOs.stream()
                .map(MatchDTO::getMatchId)
                .toList();

        CompletableFuture<Map<String, List<MatchSummonerDTO>>> summonersFuture =
                CompletableFuture.supplyAsync(() ->
                        matchRepositoryCustom.getMatchSummoners(foundIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        MatchSummonerDTO::getMatchId)),
                        queryExecutor);

        CompletableFuture<Map<String, List<TimelineEventDTO>>> timelineEventsFuture =
                CompletableFuture.supplyAsync(() ->
                        timelineRepositoryCustom
                                .selectTimelineEventsByMatchIds(foundIds)
                                .stream()
                                .collect(Collectors.groupingBy(
                                        TimelineEventDTO::getMatchId)),
                        queryExecutor);

        Map<String, List<MatchSummonerDTO>> participantsByMatch = summonersFuture.join();
        Map<String, List<TimelineEventDTO>> timelineEventsByMatch = timelineEventsFuture.join();

        return matchDTOs.stream()
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
        GameInfoReadModel gameInfoData = matchMapper.toGameInfoReadModel(matchEntity);
        gameData.setGameInfoData(gameInfoData);

        // ParticipantsData
        List<MatchSummonerDTO> summonerDTOs =
                matchRepositoryCustom.getMatchSummoners(matchEntity.getMatchId());
        List<ParticipantReadModel> participantDataList = new ArrayList<>(
                summonerDTOs.stream()
                        .map(dto -> matchMapper.toReadModel(dto))
                        .toList());
        gameData.setParticipantData(participantDataList);

        // Sorting for specific queue types
        if (gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantDataList.sort(Comparator.comparingInt(ParticipantReadModel::getPlacement));
        }

        // TimelineData → 참가자별 시퀀스 채우기
        List<TimelineEventDTO> timelineEvents =
                timelineRepositoryCustom.selectAllTimelineEventsByMatch(
                        matchEntity.getMatchId());
        applyParticipantTimelines(participantDataList, timelineEvents);

        // TeamInfoData
        Map<Integer, List<MatchSummonerDTO>> byTeam = summonerDTOs.stream()
                .collect(Collectors.groupingBy(MatchSummonerDTO::getTeamId));

        gameData.setTeamInfoData(TeamReadModel.builder()
                .blueTeam(buildTeamInfoData(byTeam.get(100)))
                .redTeam(buildTeamInfoData(byTeam.get(200)))
                .build());

        return gameData;
    }

    private static long toMinute(long timestamp) {
        return timestamp / 1000 / 60;
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
