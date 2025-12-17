package com.example.lolserver.domain.match.service;

import com.example.lolserver.domain.match.MatchMapper;
import com.example.lolserver.domain.match.command.MSChampionCommand;
import com.example.lolserver.domain.match.command.MatchCommand;
import com.example.lolserver.domain.match.domain.GameData;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.gameData.SeqTypeData;
import com.example.lolserver.domain.match.domain.gameData.TeamInfoData;
import com.example.lolserver.domain.match.domain.gameData.seqType.SeqType;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEvents;
import com.example.lolserver.repository.match.match.MatchRepository;
import com.example.lolserver.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.Page;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;

    public Page<GameData> getMatches(MatchCommand matchCommand) {

        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));

        Slice<MatchEntity> matches = matchRepositoryCustom.getMatches(
                matchCommand.getPuuid(), matchCommand.getQueueId(), pageable);

        List<GameData> gameDataList = matches.getContent().stream()
                .map(match ->
                        convertToGameData(match, matchCommand.getPuuid())).toList();

        return new Page<GameData>(gameDataList, matches.hasNext());
    }

    public List<MSChampion> getRankChampions(MSChampionCommand command) {
        List<MSChampionDTO> msChampionDTOS = matchSummonerRepositoryCustom.findAllMatchSummonerByPuuidAndSeason(
                command.getPuuid(),
                command.getSeason()
        );

        return msChampionDTOS.stream().map(
                MSChampion::of
        ).toList();
    }


    public GameData getGameData(String matchId) {
        MatchEntity match = matchRepository.findById(matchId).orElseThrow(() -> new CoreException(
                ErrorType.NOT_FOUND_MATCH_ID,
                "존재하지 않는 MatchId 입니다. " + matchId
        ));

        return convertToGameData(match, null);
    }


    public TimelineData getTimelineData(String matchId) {

        List<ItemEvents> itemEvents = timelineRepositoryCustom.selectAllItemEventsByMatch(matchId);
        List<SkillEvents> skillEvents = timelineRepositoryCustom.selectAllSkillEventsByMatch(matchId);

        return new TimelineData(itemEvents, skillEvents);
    }


    public Page<String> findAllMatchIds(MatchCommand matchCommand) {
        Pageable pageable = PageRequest.of(
                matchCommand.getPageNo(),
                20,
                Sort.by(Sort.Direction.DESC, "match")
        );
        Slice<String> matchIdsByPuuidWithPage = matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(
                matchCommand.getPuuid(), matchCommand.getQueueId(), pageable
        );

        return new Page<>(matchIdsByPuuidWithPage.getContent(), matchIdsByPuuidWithPage.hasNext());
    }

    private GameData convertToGameData(MatchEntity match, String puuid) {
        Map<Integer, Map<String, List<SeqTypeData>>> timelineDataMap = MatchMapper.domainToTimeLineDataMap(match); // Assuming getTimelineDataMap is still in Match.java

        GameData gameData = new GameData();

        // 게임 정보
        GameInfoData gameInfoData = new GameInfoData(match);
        gameData.setGameInfoData(gameInfoData);

        // 유저 정보
        List<ParticipantData> participantData = new ArrayList<>();
        for (MatchSummonerEntity matchSummoner : match.getMatchSummonerEntities()) { // Assuming matchSummoners is public or has getter
            ParticipantData data = new ParticipantData().of(matchSummoner);
            participantData.add(data);

            int participantId = data.getParticipantId();
            Map<String, List<SeqTypeData>> dataMap = timelineDataMap.get(participantId);

            if(dataMap != null) {
                data.setItemSeq(dataMap.get(SeqType.ITEM_SEQ.name()));
                data.setSkillSeq(dataMap.get(SeqType.SKILL_SEQ.name()));
            }

            if(puuid != null && data.getPuuid().equals(puuid)) {
                gameData.setMyData(data);
            }
        }

        if(gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantData.sort(Comparator.comparingInt(ParticipantData::getPlacement));
        }

        gameData.setParticipantData(participantData);

        // 팀정보
        Map<Integer, TeamInfoData> teamInfoDataMap = new HashMap<>();
//        for (MatchTeamEntity matchTeam : match.getMatchTeamEntities()) { // Assuming matchTeams is public or has getter
//            teamInfoDataMap.put(matchTeam.getTeamId(), new TeamInfoData().of(matchTeam));
//        }
        gameData.setTeamInfoData(teamInfoDataMap);

        return gameData;
    }

}
