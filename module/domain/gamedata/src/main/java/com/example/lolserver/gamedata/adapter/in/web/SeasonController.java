package com.example.lolserver.gamedata.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.gamedata.application.port.in.SeasonQueryUseCase;
import com.example.lolserver.gamedata.application.model.SeasonReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonQueryUseCase seasonService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeasonReadModel>>> getAllSeasons() {
        List<SeasonReadModel> seasons = seasonService.getAllSeasons();
        return ResponseEntity.ok(ApiResponse.success(seasons));
    }

    @GetMapping("/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonReadModel>> getSeasonById(
            @PathVariable("seasonId") Long seasonId
    ) {
        SeasonReadModel season = seasonService.getSeasonById(seasonId);
        return ResponseEntity.ok(ApiResponse.success(season));
    }
}
