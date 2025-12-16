package com.example.lolserver.controller.league;

import com.example.lolserver.controller.league.response.LeagueResponse;
import com.example.lolserver.controller.league.mapper.LeagueMapper;
import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.service.LeagueService;
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

    private final LeagueService leagueService;

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
