package com.example.lolserver.leaderboard.adapter.in.web;

import com.example.lolserver.leaderboard.application.model.readmodel.RankReadModel;
import com.example.lolserver.leaderboard.application.dto.RankSearchDto;
import com.example.lolserver.leaderboard.application.RankService;
import com.example.lolserver.common.web.response.ApiResponse;
import com.example.lolserver.common.web.response.PageResponse;
import com.example.lolserver.common.support.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    /**
     * 랭킹 리더보드
     * @param platform 플랫폼 ID (예: kr)
     * @param queue 큐 (SOLO, FLEX)
     */
    @GetMapping("/rankings")
    public ResponseEntity<ApiResponse<PageResponse<RankReadModel>>> getRankings(
            @RequestParam String platform,
            @RequestParam(defaultValue = "SOLO") RankSearchDto.GameType queue,
            @RequestParam(required = false) String tier,
            @RequestParam(defaultValue = "1") int page
    ) {
        RankSearchDto rankSearchDto = new RankSearchDto();
        rankSearchDto.setRankType(queue);
        rankSearchDto.setTier(tier);
        rankSearchDto.setPage(page);
        PageResult<RankReadModel> ranks = rankService.getRanks(rankSearchDto, platform);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(ranks)));
    }

    // legacy: lol-ui 전환 후 MP-156 에서 제거
    @GetMapping("/{platformId}/rank")
    public ResponseEntity<ApiResponse<PageResponse<RankReadModel>>> getSummonerRank(
        @PathVariable("platformId") String platformId,
        RankSearchDto rankSearchDto
    ) {
        PageResult<RankReadModel> ranks = rankService.getRanks(rankSearchDto, platformId);

        return new ResponseEntity<>(ApiResponse.success(PageResponse.of(ranks)), HttpStatus.OK);
    }

}
