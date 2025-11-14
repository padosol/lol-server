package com.example.lolserver.controller.v1;

import com.example.lolserver.domain.rank.dto.RankSearchDto;
import com.example.lolserver.domain.rank.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
