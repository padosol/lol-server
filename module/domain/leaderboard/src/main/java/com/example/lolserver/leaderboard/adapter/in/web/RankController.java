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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/{platformId}")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/rank")
    public ResponseEntity<ApiResponse<PageResponse<RankReadModel>>> getSummonerRank(
        @PathVariable("platformId") String platformId,
        RankSearchDto rankSearchDto
    ) {
        PageResult<RankReadModel> ranks = rankService.getRanks(rankSearchDto, platformId);

        return new ResponseEntity<>(ApiResponse.success(PageResponse.of(ranks)), HttpStatus.OK);
    }

}
