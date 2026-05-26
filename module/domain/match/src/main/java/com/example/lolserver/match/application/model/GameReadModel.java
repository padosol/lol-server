package com.example.lolserver.match.application.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class GameReadModel {

    // 게임정보
    private GameInfoReadModel gameInfoData;

    // 참가자 정보
    private List<ParticipantReadModel> participantData = new ArrayList<>();

    // 게임 팀 정보
    private TeamReadModel teamInfoData;

}
