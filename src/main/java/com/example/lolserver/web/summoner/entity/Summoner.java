package com.example.lolserver.web.summoner.entity;


import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.web.league.entity.LeagueSummoner;
import com.example.lolserver.web.league.entity.QueueType;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Summoner{

    @Id
    @Column(name = "summoner_id")
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

    @OneToMany(mappedBy = "summoner", fetch = FetchType.LAZY)
    private Set<LeagueSummoner> leagueSummoners = new HashSet<>();

    public Summoner(String summonerName, String region) {
        this.gameName = summonerName;
        this.region = region;
    }

    public Summoner(AccountDto account, SummonerDTO summoner, String region) {
        this.id = summoner.getId();
        this.accountId = summoner.getAccountId();
        this.profileIconId = summoner.getProfileIconId();
        this.revisionDate = summoner.getRevisionDate();
        this.summonerLevel = summoner.getSummonerLevel();

        this.puuid = account.getPuuid();
        this.gameName = account.getGameName();
        this.tagLine = account.getTagLine();

        this.region = region;

        this.revisionClickDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(summoner.getRevisionDate()), ZoneId.systemDefault());
    }

    public void splitGameNameTagLine() {
        if(StringUtils.hasText(this.gameName)) {

            String[] split = this.gameName.split("-");

            this.gameName = split[0];

            if(split.length > 1) {
                this.tagLine = split[1];
            }

        }
    }

    public SummonerResponse toResponse() {

        String tier = null;

        for (LeagueSummoner leagueSummoner : this.leagueSummoners) {
            QueueType queue = leagueSummoner.getLeague().getQueue();

            if(QueueType.RANKED_SOLO_5x5.equals(queue)) {
                tier = leagueSummoner.getLeague().getTier();
            }
        }

        return SummonerResponse.builder()
                .summonerId(this.id)
                .accountId(this.accountId)
                .summonerLevel(this.summonerLevel)
                .profileIconId(this.profileIconId)
//                .lastRevisionDateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(this.revisionDate), ZoneId.systemDefault()))
                .tier(tier)
                .lastRevisionDateTime(this.revisionClickDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .puuid(this.puuid)
                .gameName(this.gameName)
                .tagLine(this.tagLine)
                .platform(this.region)
                .build();
    }


    public boolean isRevision() {
        LocalDateTime now = LocalDateTime.now();

        return now.minusMinutes(2).isAfter(this.revisionClickDate);
    }

    public void revision(SummonerDTO summonerDTO, AccountDto accountDto) {

        this.profileIconId = summonerDTO.getProfileIconId();
        this.revisionDate = summonerDTO.getRevisionDate();
        this.summonerLevel = summonerDTO.getSummonerLevel();
        this.gameName = accountDto.getGameName();
        this.tagLine = accountDto.getTagLine();
        this.revisionClickDate = LocalDateTime.now();

    }

    public boolean isTagLine() {
        return StringUtils.hasText(this.tagLine);
    }

    public void addLeagueSummoner(Set<LeagueSummoner> leagueSummoners) {
        this.leagueSummoners = leagueSummoners;
    }

    public void resetRevisionClickDate() {
        this.revisionClickDate = LocalDateTime.now();
    }
}
