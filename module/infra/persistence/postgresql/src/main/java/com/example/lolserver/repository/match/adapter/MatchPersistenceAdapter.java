package com.example.lolserver.repository.match.adapter;

import com.example.lolserver.domain.match.application.port.out.MatchPersistencePort;
import com.example.lolserver.domain.match.domain.GameData;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.gameData.TeamInfoData;
import com.example.lolserver.domain.match.domain.gameData.timeline.ParticipantTimeline;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.SkillEvents;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.mapper.MatchMapper;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.repository.match.matchsummoner.MatchSummonerRepository;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MatchPersistenceAdapter implements MatchPersistencePort {

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchSummonerRepository matchSummonerRepository;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;

    @Override
    public Page<GameData> getMatches(String puuid, Integer queueId, Pageable pageable) {
        Slice<com.example.lolserver.repository.match.entity.MatchEntity> matchesSlice = matchRepositoryCustom.getMatches(puuid, queueId, pageable);

        List<GameData> gameDataList = matchesSlice.getContent().stream()
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
    public Optional<GameData> getGameData(String matchId) {
        return matchRepository.findById(matchId)
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
        Slice<String> matchIdsSlice = matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(puuid, queueId, pageable);
        return new Page<>(matchIdsSlice.getContent(), matchIdsSlice.hasNext());
    }


    private GameData convertToGameData(MatchEntity matchEntity, String puuid) {
        GameData gameData = new GameData();

        // GameInfoData
        GameInfoData gameInfoData = matchMapper.toGameInfoData(matchEntity);
        gameData.setGameInfoData(gameInfoData);

        // ParticipantsData
        List<ParticipantData> participantDataList = new ArrayList<>(matchSummonerRepository.findByMatchId(matchEntity.getMatchId()).stream()
                .map(matchMapper::toDomain)
                .toList());
        gameData.setParticipantData(participantDataList);

        // MyData (if puuid provided)
        if (puuid != null) {
            participantDataList.stream()
                    .filter(participant -> participant.getPuuid().equals(puuid))
                    .findFirst()
                    .ifPresent(gameData::setMyData);
        }
        
        // Sorting for specific queue types
        if(gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantDataList.sort(Comparator.comparingInt(ParticipantData::getPlacement));
        }

        // TimelineData - construct it
        // The previous MatchService had this logic, we need to replicate it here using the domain events
        // Note: The MatchMapper now provides lists of domain events for TimelineData
        List<com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity> persistenceItemEvents = timelineRepositoryCustom.selectAllItemEventsByMatch(matchEntity.getMatchId());
        List<com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity> persistenceSkillEvents = timelineRepositoryCustom.selectAllSkillEventsByMatch(matchEntity.getMatchId());

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
//        Map<Integer, TeamInfoData> teamInfoDataMap = new HashMap<>();
        // Assuming MatchEntity does not directly expose MatchTeamEntities, or it's not used in this conversion path.
        // If MatchEntity has a getMatchTeamEntities(), uncomment and map them.
//        matchEntity.getMatchTeamEntities().stream()
//                .map(matchMapper::toDomain)
//                .forEach(teamInfo -> teamInfoDataMap.put(teamInfo.getTeamId(), teamInfo));
//        gameData.setTeamInfoData(teamInfoDataMap);


        return gameData;
    }
}
