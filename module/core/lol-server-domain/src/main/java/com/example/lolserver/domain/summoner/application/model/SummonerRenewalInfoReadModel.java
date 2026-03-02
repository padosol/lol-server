package com.example.lolserver.domain.summoner.application.model;

import com.example.lolserver.domain.summoner.domain.Summoner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerRenewalInfoReadModel {
    private String puuid;
    private String gameName;
    private String tagLine;

    public static SummonerRenewalInfoReadModel of(Summoner summoner) {
        return SummonerRenewalInfoReadModel.builder()
                .puuid(summoner.getPuuid())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .build();
    }

    public static SummonerRenewalInfoReadModel ofPuuidOnly(String puuid) {
        return SummonerRenewalInfoReadModel.builder()
                .puuid(puuid)
                .build();
    }
}
