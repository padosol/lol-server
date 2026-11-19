package com.example.lolserver.esports.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.esports.adapter.in.web.response.LeagueResponse;
import com.example.lolserver.esports.adapter.in.web.response.SeasonResponse;
import com.example.lolserver.esports.adapter.in.web.response.SeasonRosterResponse;
import com.example.lolserver.esports.application.port.in.EsportsQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 공개 — 리그·시즌·시즌 로스터. 인증 없음 (SecurityConfig 의 GET /api/v1/esports/**). */
@RestController
@RequestMapping("/api/v1/esports")
@RequiredArgsConstructor
public class EsportsLeagueController {

    private final EsportsQueryUseCase esportsQueryUseCase;

    @GetMapping("/leagues")
    public ResponseEntity<ApiResponse<List<LeagueResponse>>> getLeagues() {
        return ResponseEntity.ok(ApiResponse.success(
                esportsQueryUseCase.getLeagues().stream().map(LeagueResponse::from).toList()));
    }

    @GetMapping("/leagues/{leagueId}/seasons")
    public ResponseEntity<ApiResponse<List<SeasonResponse>>> getSeasons(@PathVariable String leagueId) {
        return ResponseEntity.ok(ApiResponse.success(
                esportsQueryUseCase.getSeasons(leagueId).stream().map(SeasonResponse::from).toList()));
    }

    /** 계약 기간 ∩ 시즌 기간. 팀명은 그 시즌 당시 이름. */
    @GetMapping("/seasons/{seasonId}/rosters")
    public ResponseEntity<ApiResponse<List<SeasonRosterResponse>>> getSeasonRoster(
            @PathVariable String seasonId, @RequestParam(required = false) String teamId) {
        return ResponseEntity.ok(ApiResponse.success(esportsQueryUseCase.getSeasonRoster(seasonId, teamId).stream()
                .map(SeasonRosterResponse::from)
                .toList()));
    }
}
