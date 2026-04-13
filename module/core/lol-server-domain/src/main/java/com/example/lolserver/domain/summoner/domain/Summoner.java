package com.example.lolserver.domain.summoner.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Summoner {

    private String puuid;
    private long summonerLevel;
    private int profileIconId;
    private String gameName;
    private String tagLine;
    private String platformId;
    private String searchName;
    private LocalDateTime revisionDate;
    private LocalDateTime lastRiotCallDate;

    private List<LeagueSummoner> leagueSummoners;

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
}
