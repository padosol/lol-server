package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.rabbitmq.service.RabbitMqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class SummonerControllerV2 {

    private final RabbitMqService rabbitMqService;

    @GetMapping("/summoners/renewal/{puuid}")
    public ResponseEntity<String> renewalSummonerInfo(
            @PathVariable("puuid") String puuid
    ) {
        rabbitMqService.sendMessage(puuid);

        return ResponseEntity.ok(puuid);
    }

    @GetMapping("/match/{matchId}")
    public void messageMatch(
            @PathVariable("matchId") String matchId
    ) {
        rabbitMqService.sendMessageForMatch(matchId);
    }
}
