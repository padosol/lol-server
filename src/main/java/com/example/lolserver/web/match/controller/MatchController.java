package com.example.lolserver.web.match.controller;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.match.dto.MatchRequest;
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
    public ResponseEntity<List<GameData>> fetchGameData(
        @ModelAttribute MatchRequest matchRequest
    ) throws IOException, InterruptedException {

        List<GameData> gameData = matchService.getMatches(matchRequest);

        return new ResponseEntity<>(gameData, HttpStatus.OK);
    }

}
