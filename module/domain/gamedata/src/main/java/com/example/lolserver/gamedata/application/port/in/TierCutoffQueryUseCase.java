package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.application.model.readmodel.TierCutoffReadModel;

import java.util.List;

public interface TierCutoffQueryUseCase {

    List<TierCutoffReadModel> getTierCutoffsByRegion(String platformId);

    List<TierCutoffReadModel> getTierCutoffsByRegionAndQueue(String platformId, String queue);

    TierCutoffReadModel getTierCutoff(String platformId, String queue, String tier);

    /**
     * queue·tier 선택 필터로 티어 컷오프 목록을 조회한다. 맞는 데이터가 없으면 빈 목록.
     */
    List<TierCutoffReadModel> findTierCutoffs(String platformId, String queue, String tier);
}
