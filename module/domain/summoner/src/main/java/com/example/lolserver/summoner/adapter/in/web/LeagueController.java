package com.example.lolserver.summoner.adapter.in.web;

import com.example.lolserver.summoner.adapter.in.web.response.LeagueResponse;
import com.example.lolserver.summoner.adapter.in.web.mapper.LeagueMapper;
import com.example.lolserver.summoner.domain.League;
import com.example.lolserver.summoner.application.port.in.LeagueQueryUseCase;
import com.example.lolserver.common.web.response.ApiResponse;
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
}
