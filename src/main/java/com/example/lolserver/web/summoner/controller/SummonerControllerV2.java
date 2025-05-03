package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.rabbitmq.service.RabbitMqService;
import com.example.lolserver.web.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class SummonerControllerV2 {

    private final SummonerService summonerService;
    private final RabbitMqService rabbitMqService;

    @GetMapping("/summoners/renewal/{platform}/{puuid}")
    public ResponseEntity<String> renewalSummonerInfo(
            @PathVariable("platform") String platform,
            @PathVariable("puuid") String puuid
    ) {
        summonerService.renewalSummonerInfo(platform, puuid);
        return ResponseEntity.ok(puuid);
    }

    @GetMapping("/rabbitmq/match/{matchId}")
    public String rabbitTest(
            @PathVariable("matchId") String matchId
    ) {
        rabbitMqService.sendMessageForMatch(matchId);
        return "test";
    }

}
