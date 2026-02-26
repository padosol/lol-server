package com.example.lolserver.domain.tiercutoff.application;

import com.example.lolserver.Platform;
import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;
import com.example.lolserver.domain.tiercutoff.application.port.out.TierCutoffPersistencePort;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TierCutoffService {

    private static final Set<String> SUPPORTED_TIERS = Set.of("CHALLENGER", "GRANDMASTER");

    private final TierCutoffPersistencePort tierCutoffPersistencePort;

    public List<TierCutoffReadModel> getTierCutoffsByRegion(String platformId) {
        return tierCutoffPersistencePort.findAllByPlatformId(resolvePlatformId(platformId));
    }

    public List<TierCutoffReadModel> getTierCutoffsByRegionAndQueue(String platformId, String queue) {
        String resolvedPlatformId = resolvePlatformId(platformId);
        return tierCutoffPersistencePort.findByPlatformIdAndQueue(resolvedPlatformId, queue);
    }

    public TierCutoffReadModel getTierCutoff(String platformId, String queue, String tier) {
        String upperTier = tier.toUpperCase();
        validateTier(upperTier);

        return tierCutoffPersistencePort.findByQueueAndTierAndPlatformId(
                        queue.toUpperCase(),
                        upperTier,
                        resolvePlatformId(platformId)
                )
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_TIER_CUTOFF,
                        String.format("존재하지 않는 티어 컷오프입니다. platform: %s, queue: %s, tier: %s", platformId, queue, tier)
                ));
    }

    private String resolvePlatformId(String platformId) {
        return Platform.valueOfName(platformId).getPlatformId();
    }

    private void validateTier(String tier) {
        if (!SUPPORTED_TIERS.contains(tier)) {
            throw new CoreException(
                    ErrorType.NOT_FOUND_TIER_CUTOFF,
                    String.format("지원하지 않는 티어입니다. tier: %s (지원 티어: CHALLENGER, GRANDMASTER)", tier)
            );
        }
    }
}
