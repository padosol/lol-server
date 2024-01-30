package com.example.lolserver.riot.dto.summoner;

import lombok.Getter;
import lombok.Setter;
import org.example.entity.summoner.Summoner;

@Getter
@Setter
public class SummonerDTO {

    private String accountId;
    private int profileIconId;
    private long revisionDate;
    private String name;
    private String id;
    private String puuid;
    private long summonerLevel;

    public Summoner toEntity() {
        return Summoner.builder()
                .accountId(accountId)
                .id(id)
                .summonerLevel(summonerLevel)
                .name(name)
                .puuid(puuid)
                .profileIconId(profileIconId)
                .revisionDate(revisionDate)
                .build();
    }

}