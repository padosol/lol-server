package com.example.lolserver.web.dto.data.gameData;

import com.example.lolserver.web.dto.data.SummonerData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyData {

    private int championId;
    private String[] items;
    private int participantId;
    private String position;
    private String[] rune;
    private String[] spells;
    private String[] stats;

    // 유저 정보
    private SummonerData summonerData;

    private int teamId;
    private String tierInfo;

}
