package com.example.lolserver.web.match.controller;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.match.dto.MSChampionRequest;
import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.dto.MatchResponse;
import com.example.lolserver.web.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/v1/matches")
    public ResponseEntity<MatchResponse> fetchGameData(
        @ModelAttribute MatchRequest matchRequest
    ) {

        MatchResponse response = matchService.getMatches(matchRequest);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/v1/rank/champions")
    public ResponseEntity<List<MSChampionResponse>> getRankChampions(
            @ModelAttribute MSChampionRequest request
            ) {

        List<MSChampionResponse> result = matchService.getRankChampions(request);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
