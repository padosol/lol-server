package com.example.lolserver.domain.tiercutoff.application.port.out;

import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;

import java.util.List;
import java.util.Optional;

public interface TierCutoffPersistencePort {

    Optional<TierCutoffReadModel> findByQueueAndTierAndRegion(String queue, String tier, String region);

    List<TierCutoffReadModel> findAllByRegion(String region);

    List<TierCutoffReadModel> findByRegionAndQueue(String region, String queue);
}
