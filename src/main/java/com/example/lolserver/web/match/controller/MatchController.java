package com.example.lolserver.web.match.controller;

import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.dto.request.MatchRequest;
import com.example.lolserver.web.match.service.MatchServiceAPI;
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

    private final MatchServiceAPI matchService;

    @GetMapping("/v1/matches")
    public ResponseEntity<List<GameData>> fetchGameData(
        @ModelAttribute MatchRequest matchRequest
    ) throws IOException, InterruptedException {

        Long start = System.currentTimeMillis();
        List<GameData> gameData = matchService.getMatches(matchRequest);
        Long end = System.currentTimeMillis();

        log.info("게임 데이터 불러오는 시간: {}ms", end - start );


        return new ResponseEntity<>(gameData, HttpStatus.OK);
    }

}
