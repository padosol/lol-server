package com.example.lolserver.domain.match.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchResponse {

    private List<GameResponse> gameData;
    private Long totalCount;

    public MatchResponse() { }

    public MatchResponse(List<GameResponse> gameData, Long totalCount) {
        this.gameData = gameData;
        this.totalCount = totalCount;
    }
}
