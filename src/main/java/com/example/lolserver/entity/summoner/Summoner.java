package com.example.lolserver.entity.summoner;


import com.example.lolserver.web.dto.data.SummonerData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Summoner {

    @Id
    @Column(name = "summoner_id")
    private String id;
    private String accountId;
    private String puuid;
    private String name;
    @Column(name = "profile_icon_id")
    private int profileIconId;
    private long revisionDate;
    private long summonerLevel;

    private LocalDateTime revisionDateTime;

    private String gameName;
    private String tagLine;


    public SummonerData toData() {
        return SummonerData.builder()
                .summonerId(id)
                .accountId(accountId)
                .name(name)
                .profileIconId(profileIconId)
                .puuid(puuid)
                .revisionDate(revisionDate)
                .summonerLevel(summonerLevel)
                .gameName(gameName)
                .tagLine(tagLine)
                .build();
    }

    public void convertEpochToLocalDateTime() {
        this.revisionDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.revisionDate), ZoneOffset.UTC);
    }

    public boolean isPossibleRenewal() {

        Date now = new Date();
        Date beforeRenewalDate = new Date(this.revisionDate);

        long gap = now .getTime() - beforeRenewalDate.getTime();

        return gap >= 5 * 60 * 1000;
    }


}
