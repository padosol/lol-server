package com.example.lolserver.match.application.model.readmodel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchReadModel {

    private List<GameReadModel> gameData;
    private Long totalCount;

    public MatchReadModel() { }

    public MatchReadModel(List<GameReadModel> gameData, Long totalCount) {
        this.gameData = gameData;
        this.totalCount = totalCount;
    }
}
