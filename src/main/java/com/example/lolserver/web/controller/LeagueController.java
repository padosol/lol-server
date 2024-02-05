package com.example.lolserver.web.controller;

import com.example.lolserver.web.dto.data.LeagueData;
import com.example.lolserver.web.service.league.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;


    @GetMapping("/v1/leagues/by-summoner/{summonerId}")
    public ResponseEntity<LeagueData> fetchLeaguesBySummoner(
            @PathVariable String summonerId
    ) throws IOException, InterruptedException {

        LeagueData leagueData = leagueService.getLeaguesBySummoner(summonerId);

        return new ResponseEntity<>(leagueData, HttpStatus.OK);
    }
}
