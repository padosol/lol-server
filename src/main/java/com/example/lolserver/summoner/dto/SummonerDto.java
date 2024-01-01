package com.example.lolserver.summoner.dto;

import com.example.lolserver.summoner.entiry.Summoner;
import lombok.Data;

@Data
public class SummonerDto {

    private String accountId;
    private int profileIconId;
    private long revisionDate;
    private String name;
    private String id;
    private String puuid;
    private long summonerLevel;

    public Summoner toEntity() {
        Summoner summoner = new Summoner();
        summoner.setAccountId(accountId);
        summoner.setProfileIconId(profileIconId);
        summoner.setRevisionDate(revisionDate);
        summoner.setName(name);
        summoner.setId(id);
        summoner.setPuuid(puuid);
        summoner.setSummonerLevel(summonerLevel);

        return summoner;
    }

}
