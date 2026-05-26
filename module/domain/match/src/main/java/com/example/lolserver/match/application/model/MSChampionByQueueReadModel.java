package com.example.lolserver.match.application.model;

import com.example.lolserver.match.domain.MSChampionByQueue;

import java.util.List;

/**
 * 큐(솔로/자유)별 랭크 챔피언 통계 읽기 모델.
 */
public record MSChampionByQueueReadModel(
        List<MSChampionDetailReadModel> solo,
        List<MSChampionDetailReadModel> flex
) {
    public static MSChampionByQueueReadModel of(MSChampionByQueue domain) {
        return new MSChampionByQueueReadModel(
                domain.solo().stream().map(MSChampionDetailReadModel::of).toList(),
                domain.flex().stream().map(MSChampionDetailReadModel::of).toList()
        );
    }
}
