package com.example.lolserver.domain.match.service;

import com.example.lolserver.domain.match.dto.MSChampionRequest;
import com.example.lolserver.domain.match.dto.MatchRequest;
import com.example.lolserver.common.dto.data.GameData;
import com.example.lolserver.common.dto.data.gameData.GameInfoData;
import com.example.lolserver.common.dto.data.gameData.ParticipantData;
import com.example.lolserver.common.dto.data.gameData.SeqTypeData;
import com.example.lolserver.common.dto.data.gameData.TeamInfoData;
import com.example.lolserver.common.dto.data.gameData.seqType.SeqType;
import com.example.lolserver.common.dto.match.MSChampionDTO;
import com.example.lolserver.common.dto.match.MatchResponse;
import com.example.lolserver.storage.db.core.repository.match.entity.MatchSummoner;
import com.example.lolserver.storage.db.core.repository.match.entity.MatchTeam;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.ItemEvents;
import com.example.lolserver.storage.db.core.repository.match.entity.timeline.events.SkillEvents;
import com.example.lolserver.storage.db.core.repository.match.entity.Match;
import com.example.lolserver.storage.db.core.repository.match.match.MatchRepository;
import com.example.lolserver.storage.db.core.repository.match.match.dsl.MatchRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.match.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.example.lolserver.storage.db.core.repository.match.timeline.TimelineRepositoryCustom;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final MatchRepositoryCustom matchRepositoryCustom;
    private final TimelineRepositoryCustom timelineRepositoryCustom;
    private final MatchRepository matchRepository;

    public MatchResponse getMatches(MatchRequest matchRequest) {

        Pageable pageable = PageRequest.of(matchRequest.getPageNo(), 20, Sort.by(Sort.Direction.DESC, "match"));
        Page<Match> matches = matchRepositoryCustom.getMatches(
                matchRequest.getPuuid(), matchRequest.getQueueId(), pageable);
        List<GameData> gameDataList = matches.getContent().stream().map(match -> convertToGameData(match, matchRequest.getPuuid())).toList();

        return new MatchResponse(gameDataList, matches.getTotalElements());
    }


    public List<MSChampionDTO> getRankChampions(MSChampionRequest request) {
        return matchSummonerRepositoryCustom.findAllMatchSummonerByPuuidAndSeason(
                request.getPuuid(),
                request.getSeason()
        );
    }


    public GameData getGameData(String matchId) {
        Match match = matchRepository.findById(matchId).orElseThrow(() -> new CoreException(
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


    public List<String> findAllMatchIds(MatchRequest matchRequest) {
        Pageable pageable = PageRequest.of(
                matchRequest.getPageNo(),
                20,
                Sort.by(Sort.Direction.DESC, "match")
        );
        Page<String> matchIdsByPuuidWithPage = matchSummonerRepositoryCustom.findAllMatchIdsByPuuidWithPage(
                matchRequest.getPuuid(), matchRequest.getQueueId(), pageable
        );

        return matchIdsByPuuidWithPage.getContent();
    }

    private GameData convertToGameData(Match match, String puuid) {
        Map<Integer, Map<String, List<SeqTypeData>>> timelineDataMap = match.getTimelineDataMap(); // Assuming getTimelineDataMap is still in Match.java

        GameData gameData = new GameData();

        // 게임 정보
        GameInfoData gameInfoData = new GameInfoData(match);
        gameData.setGameInfoData(gameInfoData);

        // 유저 정보
        List<ParticipantData> participantData = new ArrayList<>();
        for (MatchSummoner matchSummoner : match.matchSummoners) { // Assuming matchSummoners is public or has getter
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
        for (MatchTeam matchTeam : match.matchTeams) { // Assuming matchTeams is public or has getter
            teamInfoDataMap.put(matchTeam.getTeamId(), new TeamInfoData().of(matchTeam));
        }
        gameData.setTeamInfoData(teamInfoDataMap);

        return gameData;
    }

}
