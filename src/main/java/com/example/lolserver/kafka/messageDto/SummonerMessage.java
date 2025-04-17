package com.example.lolserver.kafka.messageDto;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.league.entity.LeagueSummoner;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummonerMessage {

    private String id;
    private String accountId;
    private String puuid;

    private int profileIconId;
    private long revisionDate;
    private long summonerLevel;

    private String gameName;
    private String tagLine;

    private String region;

    private LocalDateTime revisionClickDate;

    private Set<LeagueSummonerMessage> leagueSummoners = new HashSet<>();

    public SummonerMessage(){};
    public SummonerMessage(Summoner summoner) {
        this.id = summoner.getId();
        this.accountId = summoner.getAccountId();
        this.puuid = summoner.getPuuid();

        this.profileIconId = summoner.getProfileIconId();
        this.revisionDate = summoner.getRevisionDate();
        this.summonerLevel = summoner.getSummonerLevel();

        this.gameName = summoner.getGameName();
        this.tagLine = summoner.getTagLine();

        this.region = summoner.getRegion();
        this.revisionClickDate = summoner.getRevisionClickDate();

        for (LeagueSummoner leagueSummoner : summoner.getLeagueSummoners()) {
            leagueSummoners.add(new LeagueSummonerMessage(leagueSummoner));
        }
    }
}
