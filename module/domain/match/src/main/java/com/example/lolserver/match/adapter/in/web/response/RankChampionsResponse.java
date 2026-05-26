package com.example.lolserver.match.adapter.in.web.response;

import com.example.lolserver.match.application.model.MSChampionByQueueReadModel;

import java.util.List;

/**
 * rank/champions API 응답 - 솔로/자유 큐별 챔피언 통계.
 */
public record RankChampionsResponse(
        List<RankChampionResponse> solo,
        List<RankChampionResponse> flex
) {
    public static RankChampionsResponse from(MSChampionByQueueReadModel readModel) {
        return new RankChampionsResponse(
                readModel.solo().stream().map(RankChampionResponse::from).toList(),
                readModel.flex().stream().map(RankChampionResponse::from).toList()
        );
    }
}
