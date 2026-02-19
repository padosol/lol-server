package com.example.lolserver.domain.summoner.application.dto;

import com.example.lolserver.domain.summoner.domain.Summoner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummonerRenewalInfoResponse {
    private String puuid;
    private String gameName;
    private String tagLine;

    public static SummonerRenewalInfoResponse of(Summoner summoner) {
        return SummonerRenewalInfoResponse.builder()
                .puuid(summoner.getPuuid())
                .gameName(summoner.getGameName())
                .tagLine(summoner.getTagLine())
                .build();
    }

    public static SummonerRenewalInfoResponse ofPuuidOnly(String puuid) {
        return SummonerRenewalInfoResponse.builder()
                .puuid(puuid)
                .build();
    }
}
