package com.example.lolserver.domain.rank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankSearchDto {

    private String platform;
    private GameType type = GameType.SOLO;
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