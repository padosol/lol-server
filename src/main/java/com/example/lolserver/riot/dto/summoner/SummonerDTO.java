package com.example.lolserver.riot.dto.summoner;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.error.ErrorDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummonerDTO extends ErrorDTO {

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

    public Summoner toEntity(AccountDto accountDto) {
        return Summoner.builder()
                .accountId(accountId)
                .id(id)
                .summonerLevel(summonerLevel)
                .name(name)
                .puuid(puuid)
                .profileIconId(profileIconId)
                .revisionDate(revisionDate)
                .gameName(accountDto.getGameName())
                .tagLine(accountDto.getTagLine())
                .build();
    }

}