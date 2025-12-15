package com.example.lolserver.common.response;

import com.example.lolserver.storage.db.core.repository.summoner.entity.Summoner;
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

    public static SummonerResponse of(Summoner summoner) {
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
