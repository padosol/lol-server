package com.example.lolserver.web.rank.controller;

import com.example.lolserver.web.rank.dto.RankResponse;
import com.example.lolserver.web.rank.dto.RankSearchDto;
import com.example.lolserver.web.rank.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    @GetMapping("/v1/rank")
    public ResponseEntity<Map<String, Object>> getSummonerRank(
        RankSearchDto rankSearchDto
    ) {
        Map<String, Object> summonerRank = rankService.getSummonerRank(rankSearchDto);

        return new ResponseEntity<>(summonerRank, HttpStatus.OK);
    }

}
