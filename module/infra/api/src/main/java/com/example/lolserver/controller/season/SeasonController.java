package com.example.lolserver.controller.season;

import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.season.application.SeasonService;
import com.example.lolserver.domain.season.application.model.SeasonReadModel;
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

    private final SeasonService seasonService;

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
