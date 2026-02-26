package com.example.lolserver.controller.tiercutoff;

import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.tiercutoff.application.TierCutoffService;
import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;
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

    private final TierCutoffService tierCutoffService;

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
