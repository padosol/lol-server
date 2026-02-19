package com.example.lolserver.repository.tiercutoff.adapter;

import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;
import com.example.lolserver.domain.tiercutoff.application.port.out.TierCutoffPersistencePort;
import com.example.lolserver.repository.tiercutoff.TierCutoffJpaRepository;
import com.example.lolserver.repository.tiercutoff.mapper.TierCutoffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TierCutoffPersistenceAdapter implements TierCutoffPersistencePort {

    private final TierCutoffJpaRepository tierCutoffJpaRepository;
    private final TierCutoffMapper tierCutoffMapper;

    @Override
    public Optional<TierCutoffReadModel> findByQueueAndTierAndRegion(String queue, String tier, String region) {
        return tierCutoffJpaRepository.findByQueueAndTierAndRegion(queue, tier, region)
                .map(tierCutoffMapper::toReadModel);
    }

    @Override
    public List<TierCutoffReadModel> findAllByRegion(String region) {
        return tierCutoffMapper.toReadModelList(
                tierCutoffJpaRepository.findAllByRegion(region)
        );
    }

    @Override
    public List<TierCutoffReadModel> findByRegionAndQueue(String region, String queue) {
        return tierCutoffMapper.toReadModelList(
                tierCutoffJpaRepository.findByRegionAndQueue(region, queue)
        );
    }
}
