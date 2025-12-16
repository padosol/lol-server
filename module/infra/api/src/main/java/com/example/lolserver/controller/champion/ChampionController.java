package com.example.lolserver.controller.champion;

import com.example.lolserver.controller.champion.response.ChampionRotateResponse;
import com.example.lolserver.domain.champion.service.ChampionService;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/{region}/champion")
@RestController
@RequiredArgsConstructor
public class ChampionController {

    private final ChampionService championService;

    @GetMapping("/rotation")
    public ResponseEntity<ApiResponse<ChampionRotateResponse>> getRotation(
            @PathVariable("region") String region
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        ChampionRotateResponse.of(championService.getRotation(region))
                ),
                HttpStatus.OK);
    }
}
