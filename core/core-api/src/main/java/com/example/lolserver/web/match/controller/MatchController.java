package com.example.lolserver.web.match.controller;

import com.example.lolserver.storage.db.core.repository.dto.data.GameData;
import com.example.lolserver.storage.db.core.repository.dto.data.TimelineData;
import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionResponse;
import com.example.lolserver.storage.db.core.repository.match.dto.MatchResponse;
import com.example.lolserver.web.match.dto.MSChampionRequest;
import com.example.lolserver.web.match.dto.MatchRequest;
import com.example.lolserver.web.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<GameData> fetchMatchResponse(
            @PathVariable("matchId") String matchId
    ) {
        GameData gameData = matchService.getGameData(matchId);

        return ResponseEntity.ok(gameData);
    }

    @GetMapping("/matches/matchIds")
    public ResponseEntity<List<String>> findAllMatchIds(
        @ModelAttribute MatchRequest matchRequest
    ) {
        List<String> allMatchIds = matchService.findAllMatchIds(matchRequest);

        return ResponseEntity.ok(allMatchIds);
    }

    @GetMapping("/matches")
    public ResponseEntity<MatchResponse> fetchGameData(
        @ModelAttribute MatchRequest matchRequest
    ) {

        MatchResponse response = matchService.getMatches(matchRequest);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/rank/champions")
    public ResponseEntity<List<MSChampionResponse>> getRankChampions(
            @ModelAttribute MSChampionRequest request
            ) {

        List<MSChampionResponse> result = matchService.getRankChampions(request);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/match/timeline/{matchId}")
    public ResponseEntity<TimelineData> getTimeline(@PathVariable("matchId") String matchId) {

        TimelineData timelineData = matchService.getTimelineData(matchId);

        return new ResponseEntity<>(timelineData, HttpStatus.OK);
    }

}
