package com.example.lolserver.esports.adapter.in.web.response;

import com.example.lolserver.esports.application.model.readmodel.SummonerLinkReadModel;

public record SummonerLinkResponse(
        String puuid,
        boolean main,
        boolean visible,
        String note
) {
    public static SummonerLinkResponse from(SummonerLinkReadModel model) {
        return new SummonerLinkResponse(
                model.puuid(),
                model.main(),
                model.visible(),
                model.note());
    }
}
