package com.example.lolserver.web.dto.data;

import com.example.lolserver.web.dto.data.gameData.GameInfoData;
import com.example.lolserver.web.dto.data.gameData.ParticipantData;
import com.example.lolserver.web.dto.data.gameData.TeamInfoData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GameData {

    // 내 정보
    private ParticipantData myData;
    
    // 게임정보
    private GameInfoData gameInfoData;

    // 참가자 정보
    private List<ParticipantData> participantData = new ArrayList<>();
    
    // 게임 팀 정보
//    private List<TeamInfoData> teamInfoData = new ArrayList<>();

    private Map<Integer, TeamInfoData> teamInfoData = new HashMap<>();

}
