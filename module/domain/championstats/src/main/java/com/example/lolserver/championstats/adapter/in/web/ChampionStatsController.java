package com.example.lolserver.championstats.adapter.in.web;

import com.example.lolserver.shared.Platform;
import com.example.lolserver.shared.TierFilter;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.championstats.application.port.in.ChampionStatsQueryUseCase;
import com.example.lolserver.championstats.application.model.readmodel.ChampionStatsReadModel;
import com.example.lolserver.championstats.application.model.readmodel.PositionChampionStatsReadModel;
import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChampionStatsController {

    private final ChampionStatsQueryUseCase championStatsService;

    /**
     * 포지션별 챔피언 통계 목록. {@code patch} 는 필수다 (MP-121).
     */
    @GetMapping("/champion-stats")
    public ResponseEntity<ApiResponse<List<PositionChampionStatsReadModel>>> getChampionStatsList(
            @RequestParam("platform") String platform,
            @RequestParam("patch") String patch,
            @RequestParam("tier") String tier
    ) {
        return ResponseEntity.ok(ApiResponse.success(championStatsService.getChampionStatsByPosition(
                patch, toRiotPlatformId(platform), parseTierFilter(tier))));
    }

    @GetMapping("/champion-stats/{championId}")
    public ResponseEntity<ApiResponse<ChampionStatsReadModel>> getChampionStatsDetail(
            @PathVariable("championId") int championId,
            @RequestParam("platform") String platform,
            @RequestParam("patch") String patch,
            @RequestParam("tier") String tier
    ) {
        return ResponseEntity.ok(ApiResponse.success(championStatsService.getChampionStats(
                championId, patch, toRiotPlatformId(platform), parseTierFilter(tier))));
    }

    private String toRiotPlatformId(String platform) {
        Platform resolved = Platform.valueOfName(platform);
        if (resolved == null) {
            throw new CoreException(ErrorType.INVALID_INPUT, "유효하지 않은 platform 입니다: " + platform);
        }
        return resolved.getPlatformId();
    }

    // ---- legacy: lol-ui 전환 후 MP-156 에서 제거 ----

    @GetMapping("/{platformId}/champion-stats")
    public ResponseEntity<ApiResponse<ChampionStatsReadModel>> getChampionStats(
            @PathVariable("platformId") String platformId,
            @RequestParam("championId") int championId,
            @RequestParam("patch") String patch,
            @RequestParam("tier") String tier
    ) {
        String riotPlatformId = toRiotPlatformId(platformId);
        TierFilter tierFilter = parseTierFilter(tier);
        ChampionStatsReadModel response = championStatsService.getChampionStats(
                championId, patch, riotPlatformId, tierFilter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{platformId}/champion-stats/positions")
    public ResponseEntity<ApiResponse<List<PositionChampionStatsReadModel>>> getChampionStatsByPosition(
            @PathVariable("platformId") String platformId,
            @RequestParam("patch") String patch,
            @RequestParam("tier") String tier
    ) {
        String riotPlatformId = toRiotPlatformId(platformId);
        TierFilter tierFilter = parseTierFilter(tier);
        List<PositionChampionStatsReadModel> response =
                championStatsService.getChampionStatsByPosition(patch, riotPlatformId, tierFilter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * URL 쿼리 파라미터에서 '+'는 공백으로 디코딩되므로 (RFC 1866),
     * "MASTER+" → "MASTER "로 수신됩니다. 앞뒤 공백을 제거한 후 trailing 공백이 있었다면
     * 원래 '+'가 있던 범위 필터로 복원합니다.
     */
    private TierFilter parseTierFilter(String tier) {
        try {
            String stripped = tier.strip();
            String normalized = tier.endsWith(" ") ? stripped + "+" : stripped;
            return TierFilter.of(normalized);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.INVALID_TIER_FILTER, e.getMessage());
        }
    }
}
