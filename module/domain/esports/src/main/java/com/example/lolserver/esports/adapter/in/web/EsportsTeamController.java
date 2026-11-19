package com.example.lolserver.esports.adapter.in.web;

import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.esports.adapter.in.web.response.TeamDetailResponse;
import com.example.lolserver.esports.adapter.in.web.response.TeamMemberResponse;
import com.example.lolserver.esports.adapter.in.web.response.TeamResponse;
import com.example.lolserver.esports.adapter.in.web.response.TeamRosterResponse;
import com.example.lolserver.esports.application.port.in.EsportsQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 공개 — 팀 목록·상세·현재 로스터·역대 인원. 인증 없음 (SecurityConfig 의 GET /api/v1/esports/**). */
@RestController
@RequestMapping("/api/v1/esports/teams")
@RequiredArgsConstructor
public class EsportsTeamController {

    private final EsportsQueryUseCase esportsQueryUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeams(
            @RequestParam(required = false) String leagueId,
            @RequestParam(defaultValue = "false") boolean includeDisbanded) {
        return ResponseEntity.ok(ApiResponse.success(esportsQueryUseCase.getTeams(leagueId, includeDisbanded)
                .stream().map(TeamResponse::from).toList()));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(@PathVariable String teamId) {
        return ResponseEntity.ok(ApiResponse.success(
                TeamDetailResponse.from(esportsQueryUseCase.getTeamDetail(teamId))));
    }

    /** 선수(포지션 순)·스태프·임대 보낸 선수. 임대 온 선수는 players 에 onLoan 으로 나온다. */
    @GetMapping("/{teamId}/roster")
    public ResponseEntity<ApiResponse<TeamRosterResponse>> getCurrentRoster(@PathVariable String teamId) {
        return ResponseEntity.ok(ApiResponse.success(
                TeamRosterResponse.from(esportsQueryUseCase.getCurrentRoster(teamId))));
    }

    /** 이 팀을 거친 사람의 소속 기간(조직 단위)과 그 안의 이 팀 구간. */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamMembers(@PathVariable String teamId) {
        return ResponseEntity.ok(ApiResponse.success(esportsQueryUseCase.getTeamMembers(teamId).stream()
                .map(TeamMemberResponse::from)
                .toList()));
    }
}
