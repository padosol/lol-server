package com.example.lolserver.esports.application.command;

/** 소환사 연결·수정. 대표 계정은 인물당 하나다. */
public record LinkSummonerCommand(
        boolean main,
        boolean visible,
        String note
) {
}
