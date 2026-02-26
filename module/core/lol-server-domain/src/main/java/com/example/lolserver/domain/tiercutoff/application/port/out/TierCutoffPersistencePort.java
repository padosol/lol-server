package com.example.lolserver.domain.tiercutoff.application.port.out;

import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;

import java.util.List;
import java.util.Optional;

public interface TierCutoffPersistencePort {

    Optional<TierCutoffReadModel> findByQueueAndTierAndPlatformId(String queue, String tier, String platformId);

    List<TierCutoffReadModel> findAllByPlatformId(String platformId);

    List<TierCutoffReadModel> findByPlatformIdAndQueue(String platformId, String queue);
}
