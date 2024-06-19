package com.example.lolserver.web.rank.controller;

import com.example.lolserver.web.rank.dto.RankResponse;
import com.example.lolserver.web.rank.dto.RankSearchDto;
import com.example.lolserver.web.rank.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/v1/rank")
    public ResponseEntity<List<RankResponse>> getSummonerRank(
        RankSearchDto rankSearchDto
    ) {
        List<RankResponse> summonerRank = rankService.getSummonerRank(rankSearchDto);

        return new ResponseEntity<>(summonerRank, HttpStatus.OK);
    }

}
