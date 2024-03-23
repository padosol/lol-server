package com.example.lolserver.entity.summoner;


import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.web.dto.data.SummonerData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

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
    @Column(name = "account_id")
    private String accountId;
    @Column(name = "puuid")
    private String puuid;

    @Column(name = "name")
    private String name;
    @Column(name = "profile_icon_id")
    private int profileIconId;
    @Column(name = "revision_date")
    private long revisionDate;
    @Column(name = "summoner_level")
    private long summonerLevel;

    @Column(name = "game_name")
    private String gameName;
    @Column(name = "tag_line")
    private String tagLine;

    @Column(name = "revision_date_time")
    private LocalDateTime revisionDateTime;

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

    public void revisionSummoner(SummonerDTO summonerDTO, AccountDto accountDto){

        this.name = summonerDTO.getName();
        this.profileIconId = summonerDTO.getProfileIconId();
        this.revisionDate = summonerDTO.getRevisionDate();
        this.summonerLevel = summonerDTO.getSummonerLevel();

        this.gameName = accountDto.getGameName();
        this.tagLine = accountDto.getTagLine();

        this.convertEpochToLocalDateTime();
    }


    public void summonerNameSetting() {

        if(StringUtils.hasText(this.name)) {

            String[] splitName = this.name.split("-");

            if(splitName.length > 1) {
                this.gameName = splitName[0];
                this.tagLine = splitName[1];
            }

        }

    }


}
