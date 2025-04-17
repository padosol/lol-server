package com.example.lolserver.web.league.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.league.service.LeagueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;

    /**
     * 소환사 리그 정보 조회 API    
     * @param summonerId 소환사 ID
     * @return 리그 정보
     * @throws IOException
     * @throws InterruptedException
     */
    @GetMapping("/v1/leagues/by-summoner/{summonerId}")
    public ResponseEntity<LeagueData> fetchLeaguesBySummoner(
            @PathVariable String summonerId
    ) throws IOException, InterruptedException {

        LeagueData leagueData = leagueService.getLeaguesBySummoner(summonerId);

        return new ResponseEntity<>(leagueData, HttpStatus.OK);
    }
}
