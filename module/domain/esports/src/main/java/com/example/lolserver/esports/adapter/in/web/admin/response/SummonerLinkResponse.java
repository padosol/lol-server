package com.example.lolserver.esports.adapter.in.web.admin.response;

import com.example.lolserver.esports.domain.PersonSummoner;

/** 소환사 연결. Riot ID·티어는 화면이 lol-repository {@code GET /api/v1/summoners/{puuid}} 로 받는다. */
public record SummonerLinkResponse(
        String puuid,
        String personId,
        boolean main,
        boolean visible,
        String note
) {
    public static SummonerLinkResponse from(PersonSummoner summoner) {
        return new SummonerLinkResponse(summoner.getPuuid(), summoner.getPersonId(), summoner.isMain(),
                summoner.isVisible(), summoner.getNote());
    }
}
