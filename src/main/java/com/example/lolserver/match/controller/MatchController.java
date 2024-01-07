package com.example.lolserver.match.controller;

import com.example.lolserver.match.dto.metadata.MatchDto;
import com.example.lolserver.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping("/v1/matches/name/{summonerName}")
    public Mono<List<MatchDto>> getSummoner(@PathVariable String summonerName) {
        return matchService.findMatchBySummonerName(summonerName);
    }

}
