package com.example.lolserver.domain.rank.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankSearchDto {

    private String region;
    private GameType rankType = GameType.SOLO;
    private int page = 1;

    private String tier;

    @Getter
    public enum GameType {
        SOLO("solo"), FLEX("flex");

        private String key;

        GameType(String key) {
            this.key = key;
        }

    }

}