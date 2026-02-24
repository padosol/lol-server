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
    @Column(name = "platform_id")
    private String platformId;
    private String searchName;
    private LocalDateTime revisionDate;
    private LocalDateTime lastRiotCallDate;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "puuid",
            referencedColumnName = "puuid",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private List<LeagueSummonerEntity> leagueSummonerEntities;

    public SummonerEntity(String summonerName, String platformId) {
        this.gameName = summonerName;
        this.platformId = platformId;
    }

    public void splitGameNameTagLine() {
        if (StringUtils.hasText(this.gameName)) {

            String[] split = this.gameName.split("-");

            this.gameName = split[0];

            if (split.length > 1) {
                this.tagLine = split[1];
            }
        }
    }

    public void clickRenewal() {
        this.lastRiotCallDate = LocalDateTime.now();
    }


    public boolean isRevision(LocalDateTime clickDateTime) {
        // 마지막 Riot API 호출로부터 2분이 경과해야 갱신 가능
        if (this.lastRiotCallDate != null
                && this.lastRiotCallDate.plusMinutes(2L).isAfter(clickDateTime)) {
            return false;
        }

        return true;
    }

    public boolean isTagLine() {
        return StringUtils.hasText(this.tagLine);
    }

}
