package com.example.lolserver.domain.summoner.domain.vo;

import org.springframework.util.StringUtils;

public record GameName(
        String summonerName,
        String tagLine
) {
    public GameName {
        if (!StringUtils.hasText(summonerName)) {
            throw new IllegalArgumentException("q 값은 null 일 수 없습니다.");
        }
    }
    public static GameName create(String q) {
        if (!StringUtils.hasText(q)) {
            throw new IllegalArgumentException("q 값은 null 일 수 없습니다.");
        }

        String[] split = q.split("-");

        String summonerName = split[0];
        String tagLine = null;
        if (split.length > 1) {
            tagLine = split[1];
        }

        return new GameName(summonerName, tagLine);
    }
}
