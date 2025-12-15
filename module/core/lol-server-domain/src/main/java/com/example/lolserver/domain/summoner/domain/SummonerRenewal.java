package com.example.lolserver.domain.summoner.domain;

import com.example.lolserver.RenewalStatus;
import com.example.lolserver.storage.redis.model.SummonerRenewalSession;
import lombok.Getter;

@Getter
public class SummonerRenewal {
    private String puuid;
    private RenewalStatus status;

    public SummonerRenewal(SummonerRenewalSession session) {
        this.puuid = session.getPuuid();
        this.status = getRenewalStatus(session);
    }

    public SummonerRenewal(String puuid,  RenewalStatus status) {
        this.puuid = puuid;
        this.status = status;
    }

    private RenewalStatus getRenewalStatus(SummonerRenewalSession session) {
        if (session.isMatchUpdate() && session.isSummonerUpdate() && session.isLeagueUpdate()) {
            return RenewalStatus.SUCCESS;
        }

        return RenewalStatus.PROGRESS;
    }
}
