package com.example.lolserver.web.match.dto;

import com.example.lolserver.web.dto.data.GameData;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchResponse {

    private List<GameData> gameData;
    private Long totalCount;

    public MatchResponse(){};

    public MatchResponse(List<GameData> gameData, Long totalCount) {
        this.gameData = gameData;
        this.totalCount = totalCount;
    }
}
