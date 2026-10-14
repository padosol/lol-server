package com.example.lolserver.summoner.domain;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
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

    public boolean isRevision(LocalDateTime clickDateTime) {
        // 마지막 Riot API 호출로부터 2분이 경과해야 갱신 가능
        if (this.lastRiotCallDate != null
                && this.lastRiotCallDate.plusMinutes(2L).isAfter(clickDateTime)) {
            return false;
        }

        return true;
    }

    /**
     * 플랫폼을 알아야 하는 작업(갱신 메시지, 관전 조회) 전에 호출한다.
     * 플랫폼이 없는 소환사는 Riot API 를 어느 지역으로 호출할지 정할 수 없다.
     */
    public void validatePlatformId() {
        if (this.platformId == null || this.platformId.isBlank()) {
            throw new CoreException(ErrorType.UNKNOWN_SUMMONER_PLATFORM,
                    "플랫폼 정보가 없는 소환사입니다. " + this.puuid);
        }
    }
}
