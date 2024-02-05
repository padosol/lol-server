package com.example.lolserver.web.controller;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.service.match.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/v1/matches/{puuid}")
    public ResponseEntity<GameData> fetchGameData(
            @PathVariable String puuid
    ) {
        GameData matches = matchService.getMatches(puuid);

        return new ResponseEntity<>(matches, HttpStatus.OK);
    }

}
