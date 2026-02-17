package com.example.lolserver.domain.match.application.dto;

import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.TeamData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GameResponse {

    // 게임정보
    private GameInfoData gameInfoData;

    // 참가자 정보
    private List<ParticipantData> participantData = new ArrayList<>();

    // 게임 팀 정보
    private TeamData teamInfoData;

}
