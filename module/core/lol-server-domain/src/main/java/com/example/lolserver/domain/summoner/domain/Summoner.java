package com.example.lolserver.domain.summoner.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Summoner {

    private String puuid;
    private long summonerLevel;
    private int profileIconId;
    private String gameName;
    private String tagLine;
    private String region;
    private String searchName;
    private LocalDateTime revisionDate;
    private LocalDateTime revisionClickDate;

    private List<LeagueSummoner> leagueSummoners;

    public void clickRenewal() {
        this.revisionClickDate = LocalDateTime.now();
    }

    public boolean isRevision(LocalDateTime clickDateTime) {
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
}
