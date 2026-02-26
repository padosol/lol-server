package com.example.lolserver.controller.championstats;

import com.example.lolserver.Platform;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.championstats.application.ChampionStatsService;
import com.example.lolserver.domain.championstats.application.dto.ChampionStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/{platformId}/champion-stats")
@RequiredArgsConstructor
public class ChampionStatsController {

    private final ChampionStatsService championStatsService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChampionStatsResponse>> getChampionStats(
            @PathVariable("platformId") String platformId,
            @RequestParam("championId") int championId,
            @RequestParam("patch") String patch,
            @RequestParam("tier") String tier
    ) {
        String riotPlatformId = Platform.valueOfName(platformId).getPlatformId();
        ChampionStatsResponse response = championStatsService.getChampionStats(
                championId, patch, riotPlatformId, tier);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
