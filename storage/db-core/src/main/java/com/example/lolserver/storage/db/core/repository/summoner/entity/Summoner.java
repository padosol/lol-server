package com.example.lolserver.storage.db.core.repository.summoner.entity;


import com.example.lolserver.riot.dto.account.AccountDto;
import com.example.lolserver.riot.dto.summoner.SummonerDTO;
import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Summoner {

    @Id
    private String puuid;
    private String summonerId;
    private String accountId;

    private int profileIconId;
    private long revisionDate;
    private long summonerLevel;

    private String gameName;
    private String tagLine;

    private String region;

    private LocalDateTime revisionClickDate;

    public Summoner(String summonerName, String region) {
        this.gameName = summonerName;
        this.region = region;
    }

    public Summoner(AccountDto account, SummonerDTO summoner, String region) {
        this.summonerId = summoner.getId();
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
        Integer points = null;
        String rank = null;

//        for (LeagueSummoner leagueSummoner : this.leagueSummoners) {
//            QueueType queue = leagueSummoner.getLeague().getQueue();
//
//            if(QueueType.RANKED_SOLO_5x5.equals(queue)) {
//                tier = leagueSummoner.getLeague().getTier();
//                points = leagueSummoner.getLeaguePoints();
//                rank = leagueSummoner.getRank();
//            }
//        }

        return SummonerResponse.builder()
                .summonerLevel(this.summonerLevel)
                .profileIconId(this.profileIconId)
                .tier(tier)
                .lastRevisionDateTime(this.revisionClickDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .puuid(this.puuid)
                .gameName(this.gameName)
                .tagLine(this.tagLine)
                .platform(this.region)
                .point(points)
                .rank(rank)
                .build();
    }

    public void clickRenewal() {
        this.revisionClickDate = LocalDateTime.now();
    }


    public boolean isRevision(LocalDateTime clickDateTime) {
        // 현재 시간이 마지막 클릭 + 10 초 한것 보다 전 인가요?
        // 전이면 갱신 불가능
        // 갱신은 10초 마다 가능
        
        // 갱신이 가능한 조건
        // 1. 갱신 시간이 3분을 넘었을 때
        // 2. 갱신 클릭 시간이 10초를 넘었을 때
        Instant instant = Instant.ofEpochMilli(this.revisionDate);
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime revisionDateTime = LocalDateTime.ofInstant(instant, zoneId);
        // 갱신 시간이 3분을 넘지 않았을 때
        if (revisionDateTime.plusMinutes(3L).isAfter(clickDateTime)) {
            return false;
        }

        // 갱신 클릭 시간이 10초를 넘지 않았을 때
        if (this.revisionClickDate.plusSeconds(10L).isAfter(clickDateTime)) {
            return false;
        }

        return true;
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

}
