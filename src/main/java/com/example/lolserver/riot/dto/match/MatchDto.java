package com.example.lolserver.riot.dto.match;

import com.example.lolserver.riot.dto.error.ErrorDTO;
import com.example.lolserver.riot.dto.match_timeline.TimelineDto;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import com.example.lolserver.web.dto.data.gameData.SeqTypeData;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import com.example.lolserver.web.match.entity.MatchSummoner;
import com.example.lolserver.web.match.entity.MatchTeam;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class MatchDto extends ErrorDTO {

    private MetadataDto metadata;
    private InfoDto info;


    private TimelineDto timeline;

    public GameData toGameData(String puuid) {

        GameData gameData = new GameData();

        // 게임 정보
        GameInfoData gameInfoData = new GameInfoData(this);
        gameData.setGameInfoData(gameInfoData);

        // 유저 정보
        List<ParticipantData> participantData = new ArrayList<>();

        for (ParticipantDto participant : this.info.getParticipants()) {
            ParticipantData data = new ParticipantData(participant);
            participantData.add(data);

            if(data.getPuuid().equals(puuid)) {
                gameData.setMyData(data);
            }
        }

        if(gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantData.sort(Comparator.comparingInt(ParticipantData::getPlacement));
        }

        gameData.setParticipantData(participantData);

        // 팀정보
        Map<Integer, TeamInfoData> teamInfoDataMap = new HashMap<>();
        for (TeamDto team : this.info.getTeams()) {
            teamInfoDataMap.put(team.getTeamId(), new TeamInfoData(team));
        }

        gameData.setTeamInfoData(teamInfoDataMap);

        return gameData;
    }

    public GameData toGameData(String puuid, TimelineDto timelineDto) {

        GameData gameData = new GameData();

        // 게임 정보
        GameInfoData gameInfoData = new GameInfoData(this);
        gameData.setGameInfoData(gameInfoData);

        // 이벤트 정보
        Map<Integer, Map<String, List<SeqTypeData>>> dataMap = timelineDto.dataCollection();

        // 유저 정보
        List<ParticipantData> participantData = new ArrayList<>();

        for (ParticipantDto participant : this.info.getParticipants()) {
            ParticipantData data = new ParticipantData(participant);
            participantData.add(data);

            Map<String, List<SeqTypeData>> seqDataMap = dataMap.get(data.getParticipantId());

            if(seqDataMap.containsKey("ITEM_PURCHASED")) {
                List<SeqTypeData> seqTypeData = seqDataMap.get("ITEM_PURCHASED");
                data.setItemSeq(seqTypeData);
            }

            if(seqDataMap.containsKey("SKILL_LEVEL_UP")) {
                List<SeqTypeData> seqTypeData = seqDataMap.get("SKILL_LEVEL_UP");
                data.setSkillSeq(seqTypeData);
            }

            if(data.getPuuid().equals(puuid)) {
                gameData.setMyData(data);
            }
        }

        if(gameData.getGameInfoData().getQueueId() == 1700 || gameData.getGameInfoData().getQueueId() == 1710) {
            participantData.sort(Comparator.comparingInt(ParticipantData::getPlacement));
        }

        gameData.setParticipantData(participantData);

        // 팀정보
        Map<Integer, TeamInfoData> teamInfoDataMap = new HashMap<>();
        for (TeamDto team : this.info.getTeams()) {
            teamInfoDataMap.put(team.getTeamId(), new TeamInfoData(team));
        }

        gameData.setTeamInfoData(teamInfoDataMap);

        return gameData;
    }

}