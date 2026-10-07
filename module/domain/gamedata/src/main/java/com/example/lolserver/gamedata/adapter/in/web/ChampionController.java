package com.example.lolserver.gamedata.adapter.in.web;

import com.example.lolserver.gamedata.adapter.in.web.response.ChampionRotateResponse;
import com.example.lolserver.gamedata.application.ChampionService;
import com.example.lolserver.common.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1")
@RestController
@RequiredArgsConstructor
public class ChampionController {

    private final ChampionService championService;

    @GetMapping("/game-data/champion-rotations")
    public ResponseEntity<ApiResponse<ChampionRotateResponse>> getChampionRotations(
            @RequestParam String platform
    ) {
        return getRotation(platform);
    }

    // legacy: lol-ui 전환 후 MP-156 에서 제거
    @GetMapping("/{platformId}/champion/rotation")
    public ResponseEntity<ApiResponse<ChampionRotateResponse>> getRotation(
            @PathVariable("platformId") String platformId
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        ChampionRotateResponse.of(championService.getChampionRotate(platformId))
                ),
                HttpStatus.OK);
    }
}
