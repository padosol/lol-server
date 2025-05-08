package com.example.lolserver.web.league.controller;

import java.io.IOException;
import java.util.List;

import com.example.lolserver.web.dto.data.leagueData.LeagueSummonerData;
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
     * @param puuid 소환사 puuid
     * @return 리그 정보
     * @throws IOException
     * @throws InterruptedException
     */
    @GetMapping("/v1/leagues/by-puuid/{puuid}")
    public ResponseEntity<List<LeagueSummonerData>> fetchLeaguesBySummoner(
            @PathVariable("puuid") String puuid
    ) {

        List<LeagueSummonerData> leagueSummonerData = leagueService.getLeaguesBypuuid(puuid);

        return new ResponseEntity<>(leagueSummonerData, HttpStatus.OK);
    }
}
