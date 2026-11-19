package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.esports.adapter.in.web.admin.request.CreateLeagueRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.CreateSeasonRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.UpdateLeagueRequest;
import com.example.lolserver.esports.adapter.in.web.admin.request.UpdateSeasonRequest;
import com.example.lolserver.esports.adapter.in.web.admin.response.LeagueResponse;
import com.example.lolserver.esports.adapter.in.web.admin.response.SeasonResponse;
import com.example.lolserver.esports.application.port.in.EsportsMasterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 — 리그·시즌. {@code /api/v1/admin/**} 는 ADMIN 만 (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/esports")
@RequiredArgsConstructor
public class AdminLeagueController {

    private final EsportsMasterUseCase esportsMasterUseCase;

    @PostMapping("/leagues")
    public ResponseEntity<ApiResponse<LeagueResponse>> createLeague(@Valid @RequestBody CreateLeagueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                LeagueResponse.from(esportsMasterUseCase.createLeague(request.toCommand()))));
    }

    @PutMapping("/leagues/{leagueId}")
    public ResponseEntity<ApiResponse<LeagueResponse>> updateLeague(
            @PathVariable String leagueId, @Valid @RequestBody UpdateLeagueRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                LeagueResponse.from(esportsMasterUseCase.updateLeague(leagueId, request.toCommand()))));
    }

    @PostMapping("/leagues/{leagueId}/seasons")
    public ResponseEntity<ApiResponse<SeasonResponse>> createSeason(
            @PathVariable String leagueId, @Valid @RequestBody CreateSeasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                SeasonResponse.from(esportsMasterUseCase.createSeason(leagueId, request.toCommand()))));
    }

    @PutMapping("/seasons/{seasonId}")
    public ResponseEntity<ApiResponse<SeasonResponse>> updateSeason(
            @PathVariable String seasonId, @Valid @RequestBody UpdateSeasonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                SeasonResponse.from(esportsMasterUseCase.updateSeason(seasonId, request.toCommand()))));
    }
}
