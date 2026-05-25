package com.example.lolserver.controller.league;

import com.example.lolserver.controller.league.response.LeagueResponse;
import com.example.lolserver.controller.league.response.LpTimelineResponse;
import com.example.lolserver.controller.league.mapper.LeagueMapper;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.application.port.in.LeagueQueryUseCase;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueQueryUseCase leagueService;

    /**
     * 소환사 리그 정보 조회 API
     * @param puuid 소환사 puuid
     * @return 리그 정보
     */
    @GetMapping("/v1/leagues/by-puuid/{puuid}")
    public ResponseEntity<ApiResponse<LeagueResponse>> fetchLeaguesBySummoner(
            @PathVariable("puuid") String puuid
    ) {
        List<League> leagues = leagueService.getLeaguesBypuuid(puuid);

        return new ResponseEntity<>(ApiResponse.success(
                LeagueMapper.domainToResponse(leagues)), HttpStatus.OK);
    }

    /**
     * 소환사 LP 변화 시계열 조회 API (솔로랭크/자유랭크 그래프 데이터)
     * @param puuid 소환사 puuid
     * @return 큐별 LP 시계열 (시간 오름차순)
     */
    @GetMapping("/v1/leagues/by-puuid/{puuid}/lp-timeline")
    public ResponseEntity<ApiResponse<LpTimelineResponse>> fetchLpTimeline(
            @PathVariable("puuid") String puuid
    ) {
        List<League> leagues = leagueService.getLpTimeline(puuid);

        return new ResponseEntity<>(ApiResponse.success(
                LeagueMapper.domainToLpTimeline(leagues)), HttpStatus.OK);
    }
}
