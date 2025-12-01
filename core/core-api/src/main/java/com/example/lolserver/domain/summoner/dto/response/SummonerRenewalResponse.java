package com.example.lolserver.domain.summoner.dto.response;

import com.example.lolserver.storage.redis.model.SummonerRenewalSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SummonerRenewalResponse {
    private String puuid;
    private RenewalStatus status;


    public static SummonerRenewalResponse of(SummonerRenewalSession session) {
        return new SummonerRenewalResponse(
                session.getPuuid(),
                getRenewalStatus(session)
        );
    }

    private static RenewalStatus getRenewalStatus(SummonerRenewalSession session) {
        if (session.isMatchUpdate() && session.isSummonerUpdate() && session.isLeagueUpdate()) {
            return RenewalStatus.SUCCESS;
        }

        return RenewalStatus.PROGRESS;
    }
}
