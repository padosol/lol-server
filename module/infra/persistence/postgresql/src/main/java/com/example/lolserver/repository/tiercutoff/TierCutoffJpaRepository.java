package com.example.lolserver.repository.tiercutoff;

import com.example.lolserver.repository.tiercutoff.entity.TierCutoffEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierCutoffJpaRepository extends JpaRepository<TierCutoffEntity, Long> {

    Optional<TierCutoffEntity> findByQueueAndTierAndPlatformId(String queue, String tier, String platformId);

    List<TierCutoffEntity> findAllByPlatformId(String platformId);

    List<TierCutoffEntity> findByPlatformIdAndQueue(String platformId, String queue);
}
