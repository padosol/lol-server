package com.example.lolserver.domain.tiercutoff.application.port.in;

import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;

import java.util.List;

public interface TierCutoffQueryUseCase {

    List<TierCutoffReadModel> getTierCutoffsByRegion(String platformId);

    List<TierCutoffReadModel> getTierCutoffsByRegionAndQueue(String platformId, String queue);

    TierCutoffReadModel getTierCutoff(String platformId, String queue, String tier);
}
