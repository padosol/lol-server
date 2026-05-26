package com.example.lolserver.gamedata.adapter.out.persistence;

import com.example.lolserver.gamedata.application.model.TierCutoffReadModel;
import com.example.lolserver.gamedata.application.port.out.TierCutoffPersistencePort;
import com.example.lolserver.gamedata.adapter.out.persistence.mapper.TierCutoffMapper;
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
