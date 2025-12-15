package com.example.lolserver.common.dto.data;

import com.example.lolserver.storage.db.core.repository.dto.data.gameData.GameInfoData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.ParticipantData;
import com.example.lolserver.storage.db.core.repository.dto.data.gameData.TeamInfoData;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
    private Map<Integer, TeamInfoData> teamInfoData = new HashMap<>();


}
