package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.rabbitmq.service.RabbitMqService;
import com.example.lolserver.web.summoner.dto.response.RenewalStatus;
import com.example.lolserver.web.summoner.dto.response.SummonerRenewalResponse;
import com.example.lolserver.web.summoner.service.SummonerService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class SummonerControllerV2 {

    private final SummonerService summonerService;

    /**
     * 소환사 전적 갱신 API
     * @param platform 플랫폼
     * @param puuid 소환사 puuid
     * @return
     */
    @GetMapping("/summoners/renewal/{platform}/{puuid}")
    public ResponseEntity<SummonerRenewalResponse> renewalSummonerInfo(
            @PathVariable(name = "platform") String platform,
            @PathVariable("puuid") String puuid
    ) {
        SummonerRenewalResponse summonerRenewalResponse = summonerService.renewalSummonerInfo(platform, puuid);
        return ResponseEntity.ok(summonerRenewalResponse);
    }
}
