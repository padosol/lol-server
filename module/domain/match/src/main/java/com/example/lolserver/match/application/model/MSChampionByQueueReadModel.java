package com.example.lolserver.match.application.model;

import java.util.List;

/**
 * 큐(솔로/자유)별 랭크 챔피언 통계 읽기 모델. 영속 어댑터가 직접 빌드한다.
 */
public record MSChampionByQueueReadModel(
        List<MSChampionDetailReadModel> solo,
        List<MSChampionDetailReadModel> flex
) {
}
