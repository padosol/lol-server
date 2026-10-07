package com.example.lolserver.gamedata.application;

import com.example.lolserver.shared.Platform;
import com.example.lolserver.shared.QueueType;
import com.example.lolserver.gamedata.application.model.readmodel.TierCutoffReadModel;
import com.example.lolserver.gamedata.application.port.in.TierCutoffQueryUseCase;
import com.example.lolserver.gamedata.application.port.out.TierCutoffPersistencePort;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TierCutoffService implements TierCutoffQueryUseCase {

    private static final Set<String> SUPPORTED_TIERS = Set.of("CHALLENGER", "GRANDMASTER");

    private final TierCutoffPersistencePort tierCutoffPersistencePort;

    public List<TierCutoffReadModel> getTierCutoffsByRegion(String platformId) {
        return tierCutoffPersistencePort.findAllByPlatformId(resolvePlatformId(platformId));
    }

    public List<TierCutoffReadModel> getTierCutoffsByRegionAndQueue(String platformId, String queue) {
        String resolvedPlatformId = resolvePlatformId(platformId);
        return tierCutoffPersistencePort.findByPlatformIdAndQueue(resolvedPlatformId, queue);
    }

    /**
     * 티어 컷오프 목록 ({@code GET /api/v1/rankings/tier-cutoffs}). queue·tier 는 선택 필터이고
     * 조건에 맞는 데이터가 없으면 빈 목록이다. tier 필터는 queue 와 함께만 쓸 수 있다.
     */
    @Override
    public List<TierCutoffReadModel> findTierCutoffs(String platformId, String queue, String tier) {
        String resolvedPlatformId = resolvePlatformId(platformId);
        if (tier == null) {
            return queue == null
                    ? tierCutoffPersistencePort.findAllByPlatformId(resolvedPlatformId)
                    : tierCutoffPersistencePort.findByPlatformIdAndQueue(resolvedPlatformId, canonicalQueue(queue));
        }
        if (queue == null) {
            throw new CoreException(ErrorType.INVALID_INPUT, "tier 필터는 queue 와 함께 지정해야 합니다.");
        }
        return tierCutoffPersistencePort.findByQueueAndTierAndPlatformId(
                        canonicalQueue(queue), tier.toUpperCase(), resolvedPlatformId)
                .map(List::of)
                .orElseGet(List::of);
    }

    /**
     * queue 를 {@link QueueType} 이름 표기로 맞춘다 (대소문자 무시). DB 조회는 대소문자를 구분하고
     * {@code RANKED_SOLO_5x5} 처럼 소문자가 섞인 이름이 있어 {@code toUpperCase()} 로는 맞출 수 없다.
     * 알 수 없는 값은 그대로 두어 빈 결과가 되게 한다.
     */
    private String canonicalQueue(String queue) {
        return Arrays.stream(QueueType.values())
                .map(QueueType::name)
                .filter(name -> name.equalsIgnoreCase(queue))
                .findFirst()
                .orElse(queue);
    }

    public TierCutoffReadModel getTierCutoff(String platformId, String queue, String tier) {
        String upperTier = tier.toUpperCase();
        validateTier(upperTier);

        return tierCutoffPersistencePort.findByQueueAndTierAndPlatformId(
                        canonicalQueue(queue),
                        upperTier,
                        resolvePlatformId(platformId)
                )
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_TIER_CUTOFF,
                        String.format("존재하지 않는 티어 컷오프입니다. platform: %s, queue: %s, tier: %s", platformId, queue, tier)
                ));
    }

    private String resolvePlatformId(String platformId) {
        Platform platform = Platform.valueOfName(platformId);
        if (platform == null) {
            throw new CoreException(ErrorType.INVALID_INPUT, "유효하지 않은 platform 입니다: " + platformId);
        }
        return platform.getPlatformId();
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
