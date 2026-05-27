package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.readmodel.MSChampionDetailReadModel;

/**
 * rank/champions API 응답 - 챔피언 단건 통계.
 * 필드 구성은 API 계약이며, application 의 ReadModel 변경과 독립적으로 유지된다.
 */
public record RankChampionResponse(
        Double assists,
        Double deaths,
        Double kills,
        int championId,
        String championName,
        Long win,
        Long losses,
        Double winRate,
        Double damagePerMinute,
        Double kda,
        Double laneMinionsFirst10Minutes,
        Double damageTakenOnTeamPercentage,
        Double goldPerMinute,
        Long playCount
) {
    public static RankChampionResponse from(MSChampionDetailReadModel model) {
        return new RankChampionResponse(
                model.getAssists(),
                model.getDeaths(),
                model.getKills(),
                model.getChampionId(),
                model.getChampionName(),
                model.getWin(),
                model.getLosses(),
                model.getWinRate(),
                model.getDamagePerMinute(),
                model.getKda(),
                model.getLaneMinionsFirst10Minutes(),
                model.getDamageTakenOnTeamPercentage(),
                model.getGoldPerMinute(),
                model.getPlayCount()
        );
    }
}
