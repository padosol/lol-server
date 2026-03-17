package com.example.lolserver.controller.championstats;

import com.example.lolserver.Platform;
import com.example.lolserver.TierFilter;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.championstats.application.ChampionStatsService;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import com.example.lolserver.support.error.CoreException;
import com.example.lolserver.support.error.ErrorType;
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
@RequestMapping("/api/v1/{platformId}/champion-stats")
@RequiredArgsConstructor
public class ChampionStatsController {

    private final ChampionStatsService championStatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChampionStatsReadModel>> getChampionStats(
            @PathVariable("platformId") String platformId,
            @RequestParam("championId") int championId,
            @RequestParam("patch") String patch,
            @RequestParam("tier") String tier
    ) {
        String riotPlatformId = Platform.valueOfName(platformId).getPlatformId();
        TierFilter tierFilter = parseTierFilter(tier);
        ChampionStatsReadModel response = championStatsService.getChampionStats(
                championId, patch, riotPlatformId, tierFilter);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/positions")
    public ResponseEntity<ApiResponse<List<PositionChampionStatsReadModel>>> getChampionStatsByPosition(
            @PathVariable("platformId") String platformId,
            @RequestParam("patch") String patch,
            @RequestParam("tier") String tier
    ) {
        String riotPlatformId = Platform.valueOfName(platformId).getPlatformId();
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
