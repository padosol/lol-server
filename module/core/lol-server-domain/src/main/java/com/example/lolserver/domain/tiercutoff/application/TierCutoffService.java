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

    public List<TierCutoffReadModel> getTierCutoffsByRegion(String platform) {
        return tierCutoffPersistencePort.findAllByRegion(resolveRegion(platform));
    }

    public List<TierCutoffReadModel> getTierCutoffsByRegionAndQueue(String platform, String queue) {
        String region = resolveRegion(platform);
        return tierCutoffPersistencePort.findByRegionAndQueue(region, queue.toUpperCase());
    }

    public TierCutoffReadModel getTierCutoff(String platform, String queue, String tier) {
        String upperTier = tier.toUpperCase();
        validateTier(upperTier);

        return tierCutoffPersistencePort.findByQueueAndTierAndRegion(
                        queue.toUpperCase(),
                        upperTier,
                        resolveRegion(platform)
                )
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_TIER_CUTOFF,
                        String.format("존재하지 않는 티어 컷오프입니다. platform: %s, queue: %s, tier: %s", platform, queue, tier)
                ));
    }

    private String resolveRegion(String platform) {
        return Platform.valueOfName(platform).getRegion();
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
