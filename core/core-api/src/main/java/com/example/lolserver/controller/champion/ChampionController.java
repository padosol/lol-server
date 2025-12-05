package com.example.lolserver.controller.champion;

import com.example.lolserver.domain.champion.service.ChampionService;
import com.example.lolserver.riot.dto.champion.ChampionInfo;
import com.example.lolserver.support.response.ApiResponse;
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
    public ResponseEntity<ApiResponse<ChampionInfo>> getRotation(
            @PathVariable("region") String region
    ) {

        ChampionInfo rotation = championService.getRotation(region);

        return new ResponseEntity<>(ApiResponse.success(rotation), HttpStatus.OK);
    }
}
