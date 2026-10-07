package com.example.lolserver.gamedata.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.gamedata.application.port.in.TierCutoffQueryUseCase;
import com.example.lolserver.gamedata.application.model.readmodel.TierCutoffReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TierCutoffController {

    private final TierCutoffQueryUseCase tierCutoffService;

    /**
     * 티어 컷오프 목록. 티어 컷오프는 매일 바뀌는 랭킹 데이터라 {@code rankings} 아래에 둔다.
     * @param platform 플랫폼 ID (예: kr)
     * @param queue 큐 타입 필터 (선택)
     * @param tier 티어 필터 (선택, queue 와 함께 줘야 한다)
     */
    @GetMapping("/v1/rankings/tier-cutoffs")
    public ResponseEntity<ApiResponse<List<TierCutoffReadModel>>> getRankingTierCutoffs(
            @RequestParam String platform,
            @RequestParam(required = false) String queue,
            @RequestParam(required = false) String tier
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                tierCutoffService.findTierCutoffs(platform, queue, tier)));
    }

    // ---- legacy: lol-ui 전환 후 MP-156 에서 제거 ----

    /**
     * 지역별 티어 컷오프 목록 조회 API
     * @param platformId 플랫폼 ID (예: kr, na)
     * @param queue 큐 타입 필터 (선택, 예: RANKED_SOLO_5x5)
     * @return 티어 컷오프 목록
     */
    @GetMapping("/v1/{platformId}/tier-cutoffs")
    public ResponseEntity<ApiResponse<List<TierCutoffReadModel>>> getTierCutoffs(
            @PathVariable String platformId,
            @RequestParam(required = false) String queue
    ) {
        log.info("getTierCutoffs - platformId: {}, queue: {}", platformId, queue);

        List<TierCutoffReadModel> tierCutoffs = queue != null
                ? tierCutoffService.getTierCutoffsByRegionAndQueue(platformId, queue)
                : tierCutoffService.getTierCutoffsByRegion(platformId);

        return ResponseEntity.ok(ApiResponse.success(tierCutoffs));
    }

    /**
     * 특정 티어 컷오프 상세 조회 API
     * @param platformId 플랫폼 ID (예: kr, na)
     * @param queue 큐 타입 (예: RANKED_SOLO_5x5)
     * @param tier 티어 (CHALLENGER, GRANDMASTER)
     * @return 티어 컷오프 상세 정보
     */
    @GetMapping("/v1/{platformId}/tier-cutoffs/{queue}/{tier}")
    public ResponseEntity<ApiResponse<TierCutoffReadModel>> getTierCutoff(
            @PathVariable String platformId,
            @PathVariable String queue,
            @PathVariable String tier
    ) {
        log.info("getTierCutoff - platformId: {}, queue: {}, tier: {}", platformId, queue, tier);

        TierCutoffReadModel tierCutoff = tierCutoffService.getTierCutoff(platformId, queue, tier);

        return ResponseEntity.ok(ApiResponse.success(tierCutoff));
    }
}
