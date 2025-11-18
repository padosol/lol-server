package com.example.lolserver.controller;

import com.example.lolserver.domain.spectator.service.SpectatorService;
import com.example.lolserver.riot.dto.spectator.CurrentGameInfo;
import com.example.lolserver.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class SpectatorController {

    private final SpectatorService spectatorServiceV1;

    @GetMapping("/spectator/{puuid}/{region}")
    public ResponseEntity<ApiResponse<CurrentGameInfo>> getCurrentGameInfo(
            @PathVariable("puuid") String puuid,
            @PathVariable("region") String region
    ) {

        CurrentGameInfo currentGameInfo = spectatorServiceV1.getCurrentGameInfo(puuid, region);

        return ResponseEntity.ok(ApiResponse.success(currentGameInfo));
    }
}

