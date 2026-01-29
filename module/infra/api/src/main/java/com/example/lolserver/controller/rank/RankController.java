package com.example.lolserver.controller.rank;

import com.example.lolserver.domain.rank.application.dto.RankResponse;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.application.RankService;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{region}")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/rank")
    public ResponseEntity<ApiResponse<List<RankResponse>>> getSummonerRank(
        @PathVariable("region") String region,
        RankSearchDto rankSearchDto
    ) {
        rankSearchDto.setRegion(region);
        List<RankResponse> ranks = rankService.getRanks(rankSearchDto);

        return new ResponseEntity<>(ApiResponse.success(ranks), HttpStatus.OK);
    }

}
