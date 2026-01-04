package com.example.lolserver.controller.rank;

import com.example.lolserver.domain.rank.application.dto.RankResponse;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.application.RankService;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/v1/rank")
    public ResponseEntity<ApiResponse<List<RankResponse>>> getSummonerRank(
        RankSearchDto rankSearchDto
    ) {
        List<RankResponse> ranks = rankService.getRanks(rankSearchDto);

        return new ResponseEntity<>(ApiResponse.success(ranks), HttpStatus.OK);
    }

}
