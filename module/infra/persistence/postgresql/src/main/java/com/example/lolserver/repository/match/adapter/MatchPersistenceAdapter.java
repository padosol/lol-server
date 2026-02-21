package com.example.lolserver.repository.match.adapter;

import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.application.dto.GameResponse;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.domain.gamedata.GameInfoData;
import com.example.lolserver.domain.match.domain.gamedata.ParticipantData;
import com.example.lolserver.domain.match.domain.gamedata.TeamInfoData;
import com.example.lolserver.domain.match.domain.TeamData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.ParticipantTimeline;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.SkillEvents;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public Page<GameResponse> getMatches(String puuid, Integer queueId, Pageable pageable) {
        Slice<MatchEntity> matchesSlice =
                matchRepositoryCustom.getMatches(puuid, queueId, pageable);

        List<GameResponse> gameDataList = matchesSlice.getContent().stream()
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
    public Optional<GameResponse> getGameData(String matchId) {
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
    public Page<GameResponse> getMatchesBatch(String puuid, Integer queueId, Pageable pageable) {
        Slice<MatchEntity> matchesSlice = matchRepositoryCustom.getMatches(puuid, queueId, pageable);
        List<MatchEntity> matchEntities = matchesSlice.getContent();

        if (matchEntities.isEmpty()) {
            return new Page<>(Collections.emptyList(), false);
        }

        List<String> matchIds = matchEntities.stream()
                .map(MatchEntity::getMatchId)
                .toList();

        // 배치 쿼리: 4개 쿼리로 모든 데이터 로딩
        Map<String, List<MatchSummonerEntity>> participantsByMatch =
                matchSummonerRepository.findByMatchIdIn(matchIds).stream()
                        .collect(Collectors.groupingBy(
                                MatchSummonerEntity::getMatchId));

        Map<String, List<MatchTeamEntity>> teamsByMatch =
                matchTeamRepository.findByMatchIdIn(matchIds).stream()
                        .collect(Collectors.groupingBy(MatchTeamEntity::getMatchId));

        Map<String, List<ItemEventsEntity>> itemEventsByMatch =
                timelineRepositoryCustom.selectAllItemEventsByMatchIds(matchIds).stream()
                        .collect(Collectors.groupingBy(ie -> ie.getTimeLineEvent().getMatchId()));

        Map<String, List<SkillEventsEntity>> skillEventsByMatch =
                timelineRepositoryCustom.selectAllSkillEventsByMatchIds(matchIds).stream()
                        .collect(Collectors.groupingBy(se -> se.getTimeLineEvent().getMatchId()));

        // 매치별 GameResponse 조립 (DB 호출 없이 메모리에서)
        List<GameResponse> gameDataList = matchEntities.stream()
                .map(matchEntity -> assembleGameData(
                        matchEntity, puuid,
                        participantsByMatch.getOrDefault(matchEntity.getMatchId(), Collections.emptyList()),
                        teamsByMatch.getOrDefault(matchEntity.getMatchId(), Collections.emptyList()),
                        itemEventsByMatch.getOrDefault(matchEntity.getMatchId(), Collections.emptyList()),
                        skillEventsByMatch.getOrDefault(matchEntity.getMatchId(), Collections.emptyList())
                ))
                .toList();

        return new Page<>(gameDataList, matchesSlice.hasNext());
    }

    private GameResponse assembleGameData(
            MatchEntity matchEntity,
            String puuid,
            List<MatchSummonerEntity> summonerEntities,
            List<MatchTeamEntity> teamEntities,
            List<ItemEventsEntity> itemEvents,
            List<SkillEventsEntity> skillEvents
    ) {
        GameResponse gameData = new GameResponse();

        GameInfoData gameInfoData = matchMapper.toGameInfoData(matchEntity);
        gameData.setGameInfoData(gameInfoData);

        List<ParticipantData> participantDataList = new ArrayList<>(summonerEntities.stream()
                .map(matchMapper::toDomain)
                .toList());
        gameData.setParticipantData(participantDataList);

        if (gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantDataList.sort(Comparator.comparingInt(ParticipantData::getPlacement));
        }

        List<ItemEvents> domainItemEvents = matchMapper.toDomainItemEventsList(itemEvents);
        List<SkillEvents> domainSkillEvents = matchMapper.toDomainSkillEventsList(skillEvents);
        TimelineData timelineData = new TimelineData(domainItemEvents, domainSkillEvents);

        for (ParticipantData participant : participantDataList) {
            int participantId = participant.getParticipantId();
            ParticipantTimeline participantTimeline = timelineData.getParticipantTimeline(participantId);
            if (participantTimeline != null) {
                participant.setItemSeq(participantTimeline.getItemSeq());
                participant.setSkillSeq(participantTimeline.getSkillSeq());
            }
        }

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

    private GameResponse convertToGameData(MatchEntity matchEntity, String puuid) {
        GameResponse gameData = new GameResponse();

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
