package com.example.lolserver.controller.match;

import com.example.lolserver.domain.match.application.command.MSChampionCommand;
import com.example.lolserver.domain.match.application.command.MatchCommand;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.application.MatchService;
import com.example.lolserver.domain.match.application.dto.DailyGameCountResponse;
import com.example.lolserver.domain.match.application.dto.GameResponse;
import com.example.lolserver.domain.match.domain.TimelineData;
import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.controller.support.response.SliceResponse;
import com.example.lolserver.support.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<ApiResponse<GameResponse>> fetchMatchResponse(
            @PathVariable("matchId") String matchId
    ) {
        GameResponse gameData = matchService.getGameData(matchId);

        return ResponseEntity.ok(ApiResponse.success(gameData));
    }

    @GetMapping("/matches/matchIds")
    public ResponseEntity<ApiResponse<SliceResponse<String>>> findAllMatchIds(
        @ModelAttribute MatchCommand matchCommand
    ) {
        Page<String> allMatchIds = matchService.findAllMatchIds(matchCommand);

        return ResponseEntity.ok(ApiResponse.success(SliceResponse.of(allMatchIds)));
    }

    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<SliceResponse<GameResponse>>> fetchGameResponse(
        @ModelAttribute MatchCommand matchCommand
    ) {
        Page<GameResponse> matches = matchService.getMatches(matchCommand);

        return ResponseEntity.ok(ApiResponse.success(SliceResponse.of(matches)));
    }

    @GetMapping("/rank/champions")
    public ResponseEntity<ApiResponse<List<MSChampion>>> getRankChampions(
            @ModelAttribute MSChampionCommand request) {

        List<MSChampion> result = matchService.getRankChampions(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        result));
    }

    @GetMapping("/summoners/{puuid}/matches")
    public ResponseEntity<ApiResponse<SliceResponse<GameResponse>>> fetchMatchesBySummoner(
            @PathVariable("puuid") String puuid,
            @RequestParam(required = false) Integer queueId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) String region
    ) {
        MatchCommand matchCommand = MatchCommand.builder()
                .puuid(puuid)
                .queueId(queueId)
                .pageNo(pageNo != null ? pageNo : 1)
                .region(region)
                .build();
        Page<GameResponse> matches = matchService.getMatchesBatch(matchCommand);
        return ResponseEntity.ok(ApiResponse.success(SliceResponse.of(matches)));
    }

    @GetMapping("/summoners/{puuid}/matches/daily-count")
    public ResponseEntity<ApiResponse<List<DailyGameCountResponse>>> getDailyGameCounts(
            @PathVariable("puuid") String puuid,
            @RequestParam Integer season,
            @RequestParam(required = false) Integer queueId) {
        List<DailyGameCountResponse> result =
                matchService.getDailyGameCounts(puuid, season, queueId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/match/timeline/{matchId}")
    public ResponseEntity<ApiResponse<TimelineData>> getTimeline(@PathVariable("matchId") String matchId) {

        TimelineData timelineData = matchService.getTimelineData(matchId);

        return ResponseEntity.ok(ApiResponse.success(timelineData));
    }

}
