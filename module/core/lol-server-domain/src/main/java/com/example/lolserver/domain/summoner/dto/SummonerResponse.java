package com.example.lolserver.domain.summoner.dto;

import com.example.lolserver.repository.summoner.entity.SummonerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerResponse {
    private int profileIconId;
    private String puuid;
    private long summonerLevel;
    private String gameName;
    private String tagLine;
    private String platform;
    private String lastRevisionDateTime;
    private String lastRevisionClickDateTime;

    public static SummonerResponse of(SummonerEntity summoner) {
        return SummonerResponse.builder()
                .profileIconId(summoner.getProfileIconId())
                .puuid(summoner.getPuuid())
                .summonerLevel(summoner.getSummonerLevel())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .lastRevisionDateTime(summoner.getRevisionDate().toString())
                .lastRevisionClickDateTime(summoner.getRevisionClickDate().toString())
                .build();
    }
}
