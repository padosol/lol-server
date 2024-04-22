package com.example.lolserver.web.match.controller;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.match.service.MatchServiceAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchController {

    private final MatchServiceAPI matchService;

    @GetMapping("/v1/matches")
    public ResponseEntity<List<GameData>> fetchGameData(
        @ModelAttribute MatchRequest matchRequest
    ) throws IOException, InterruptedException {

        List<GameData> gameData = matchService.getMatches(matchRequest);

        return new ResponseEntity<>(gameData, HttpStatus.OK);
    }

}
