package com.example.lolserver.esports.application.model.readmodel;

/** 소환사 연결 (puuid 만. 소환사 정보는 화면이 lol-repository 에서 받는다). */
public record SummonerLinkReadModel(
        String puuid,
        boolean main,
        boolean visible,
        String note
) {
}
