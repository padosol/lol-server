package com.example.lolserver.controller;

import com.example.lolserver.domain.match.dto.MSChampionRequest;
import com.example.lolserver.domain.match.dto.MatchRequest;
import com.example.lolserver.domain.match.service.MatchService;
import com.example.lolserver.storage.db.core.repository.dto.data.GameData;
import com.example.lolserver.storage.db.core.repository.dto.data.TimelineData;
import com.example.lolserver.storage.db.core.repository.match.dto.MSChampionResponse;
import com.example.lolserver.storage.db.core.repository.match.dto.MatchResponse;
import com.example.lolserver.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiResponse<GameData>> fetchMatchResponse(
            @PathVariable("matchId") String matchId
    ) {
        GameData gameData = matchService.getGameData(matchId);

        return ResponseEntity.ok(ApiResponse.success(gameData));
    }

    @GetMapping("/matches/matchIds")
    public ResponseEntity<ApiResponse<List<String>>> findAllMatchIds(
        @ModelAttribute MatchRequest matchRequest
    ) {
        List<String> allMatchIds = matchService.findAllMatchIds(matchRequest);

        return ResponseEntity.ok(ApiResponse.success(allMatchIds));
    }

    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<MatchResponse>> fetchGameData(
        @ModelAttribute MatchRequest matchRequest
    ) {
        MatchResponse response = matchService.getMatches(matchRequest);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/rank/champions")
    public ResponseEntity<ApiResponse<List<MSChampionResponse>>> getRankChampions(
            @ModelAttribute MSChampionRequest request
            ) {

        List<MSChampionResponse> result = matchService.getRankChampions(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/match/timeline/{matchId}")
    public ResponseEntity<ApiResponse<TimelineData>> getTimeline(@PathVariable("matchId") String matchId) {

        TimelineData timelineData = matchService.getTimelineData(matchId);

        return ResponseEntity.ok(ApiResponse.success(timelineData));
    }

}
