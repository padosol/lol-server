package com.example.lolserver.summoner.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.summoner.application.port.in.SpectatorQueryUseCase;
import com.example.lolserver.summoner.application.model.readmodel.CurrentGameInfoReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SpectatorController {

    private final SpectatorQueryUseCase spectatorService;

    @GetMapping("/v1/summoners/{puuid}/active-game")
    public ResponseEntity<ApiResponse<CurrentGameInfoReadModel>> getActiveGame(
            @PathVariable("puuid") String puuid
    ) {
        return ResponseEntity.ok(ApiResponse.success(spectatorService.getCurrentGameInfo(puuid)));
    }

    // legacy: lol-ui 전환 후 MP-156 에서 제거
    @GetMapping("/v1/{platformId}/spectator/active-games/by-puuid/{puuid}")
    public ResponseEntity<ApiResponse<CurrentGameInfoReadModel>> getCurrentGameInfo(
            @PathVariable("platformId") String platformId,
            @PathVariable("puuid") String puuid
    ) {
        CurrentGameInfoReadModel gameInfo = spectatorService.getCurrentGameInfo(puuid, platformId);
        return ResponseEntity.ok(ApiResponse.success(gameInfo));
    }

}
