package com.example.lolserver.match.adapter.in.web;

import com.example.lolserver.match.application.command.MSChampionCommand;
import com.example.lolserver.match.application.command.MatchCommand;
import com.example.lolserver.match.adapter.in.web.response.DailyGameCountResponse;
import com.example.lolserver.match.adapter.in.web.response.GameResponse;
import com.example.lolserver.match.adapter.in.web.response.RankChampionsResponse;
import com.example.lolserver.match.adapter.in.web.response.TimelineResponse;
import com.example.lolserver.match.application.port.in.MatchQueryUseCase;
import com.example.lolserver.match.application.model.readmodel.GameReadModel;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.response.SliceResponse;
import com.example.lolserver.common.support.SliceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MatchController {

    private final MatchQueryUseCase matchService;

    @GetMapping("/matches/{matchId}")
    public ResponseEntity<ApiResponse<GameResponse>> fetchMatchResponse(
            @PathVariable("matchId") String matchId) {
        GameResponse gameData = GameResponse.from(matchService.getGameData(matchId));

        return ResponseEntity.ok(ApiResponse.success(gameData));
    }

    @GetMapping("/{platformId}/matches/matchIds")
    public ResponseEntity<ApiResponse<SliceResponse<String>>> findAllMatchIds(
            @PathVariable("platformId") String platformId,
            @ModelAttribute MatchCommand matchCommand) {
        matchCommand.setPlatformId(platformId);
        SliceResult<String> allMatchIds = matchService.findAllMatchIds(matchCommand);

        return ResponseEntity.ok(ApiResponse.success(SliceResponse.of(allMatchIds)));
    }

    @GetMapping("/{platformId}/matches")
    public ResponseEntity<ApiResponse<SliceResponse<GameResponse>>> fetchGameReadModel(
            @PathVariable("platformId") String platformId,
            @ModelAttribute MatchCommand matchCommand) {
        matchCommand.setPlatformId(platformId);
        SliceResult<GameReadModel> matches = matchService.getMatches(matchCommand);

        return ResponseEntity.ok(ApiResponse.success(toGameResponseSlice(matches)));
    }

    @GetMapping("/rank/champions")
    public ResponseEntity<ApiResponse<RankChampionsResponse>> getRankChampions(
            @ModelAttribute MSChampionCommand request) {
        RankChampionsResponse result = RankChampionsResponse.from(matchService.getRankChampions(request));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{platformId}/summoners/{puuid}/matches")
    public ResponseEntity<ApiResponse<SliceResponse<GameResponse>>> fetchMatchesBySummoner(
            @PathVariable("platformId") String platformId,
            @PathVariable("puuid") String puuid,
            @RequestParam(required = false) Integer season,
            @RequestParam(required = false) Integer queueId,
            @RequestParam(required = false) Integer pageNo) {
        MatchCommand matchCommand = MatchCommand.builder()
                .puuid(puuid)
                .season(season)
                .queueId(queueId)
                .pageNo(pageNo != null ? pageNo : 1)
                .platformId(platformId)
                .build();
        SliceResult<GameReadModel> matches = matchService.getMatchesBatch(matchCommand);
        return ResponseEntity.ok(ApiResponse.success(toGameResponseSlice(matches)));
    }

    @GetMapping("/{platformId}/summoners/{puuid}/matches/daily-count")
    public ResponseEntity<ApiResponse<DailyGameCountResponse>> getDailyGameCounts(
            @PathVariable("platformId") String platformId,
            @PathVariable("puuid") String puuid,
            @RequestParam Integer season,
            @RequestParam(required = false) Integer queueId) {
        DailyGameCountResponse result =
                DailyGameCountResponse.from(matchService.getDailyGameCounts(puuid, season, queueId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/match/timeline/{matchId}")
    public ResponseEntity<ApiResponse<TimelineResponse>> getTimeline(
            @PathVariable("matchId") String matchId) {
        TimelineResponse timelineData = TimelineResponse.from(matchService.getTimelineData(matchId));

        return ResponseEntity.ok(ApiResponse.success(timelineData));
    }

    private SliceResponse<GameResponse> toGameResponseSlice(SliceResult<GameReadModel> matches) {
        return new SliceResponse<>(
                matches.getContent().stream().map(GameResponse::from).toList(),
                matches.isHasNext());
    }

}
