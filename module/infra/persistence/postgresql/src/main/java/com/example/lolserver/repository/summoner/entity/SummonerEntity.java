package com.example.lolserver.repository.summoner.entity;


import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "summoner")
public class SummonerEntity {

    @Id
    private String puuid;
    private long summonerLevel;
    private int profileIconId;
    private String gameName;
    private String tagLine;
    private String region;
    private String searchName;
    private LocalDateTime revisionDate;
    private LocalDateTime revisionClickDate;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "puuid",
            referencedColumnName = "puuid",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private List<LeagueSummonerEntity> leagueSummonerEntities;

    public SummonerEntity(String summonerName, String region) {
        this.gameName = summonerName;
        this.region = region;
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
        // 갱신 시간이 3분을 넘지 않았을 때
        if (this.revisionDate.plusMinutes(3L).isAfter(clickDateTime)) {
            return false;
        }

        // 갱신 클릭 시간이 10초를 넘지 않았을 때
        if (this.revisionClickDate.plusSeconds(10L).isAfter(clickDateTime)) {
            return false;
        }

        return true;
    }

//    public void revision(SummonerDTO summonerDTO, AccountDto accountDto) {
//
//        this.profileIconId = summonerDTO.getProfileIconId();
//
//        Instant instant = Instant.ofEpochMilli(summonerDTO.getRevisionDate());
//        ZoneId zoneId = ZoneId.systemDefault();
//        this.revisionDate = LocalDateTime.ofInstant(instant, zoneId);
//
//        this.summonerLevel = summonerDTO.getSummonerLevel();
//        this.gameName = accountDto.getGameName();
//        this.tagLine = accountDto.getTagLine();
//        this.revisionClickDate = LocalDateTime.now();
//    }

    public boolean isTagLine() {
        return StringUtils.hasText(this.tagLine);
    }

}
