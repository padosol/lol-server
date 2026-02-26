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
    public Optional<TierCutoffReadModel> findByQueueAndTierAndPlatformId(String queue, String tier, String platformId) {
        return tierCutoffJpaRepository.findByQueueAndTierAndPlatformId(queue, tier, platformId)
                .map(tierCutoffMapper::toReadModel);
    }

    @Override
    public List<TierCutoffReadModel> findAllByPlatformId(String platformId) {
        return tierCutoffMapper.toReadModelList(
                tierCutoffJpaRepository.findAllByPlatformId(platformId)
        );
    }

    @Override
    public List<TierCutoffReadModel> findByPlatformIdAndQueue(String platformId, String queue) {
        return tierCutoffMapper.toReadModelList(
                tierCutoffJpaRepository.findByPlatformIdAndQueue(platformId, queue)
        );
    }
}
