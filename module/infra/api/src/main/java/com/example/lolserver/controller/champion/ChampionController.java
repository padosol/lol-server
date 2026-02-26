package com.example.lolserver.controller.champion;

import com.example.lolserver.controller.champion.response.ChampionRotateResponse;
import com.example.lolserver.domain.champion.application.ChampionService;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/{platformId}/champion")
@RestController
@RequiredArgsConstructor
public class ChampionController {

    private final ChampionService championService;

    @GetMapping("/rotation")
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
