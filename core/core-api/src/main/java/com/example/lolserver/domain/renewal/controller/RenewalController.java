package com.example.lolserver.domain.renewal.controller;

import com.example.lolserver.domain.renewal.dto.response.SummonerRenewalStatus;
import com.example.lolserver.domain.renewal.service.RenewalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RenewalController {

    private final RenewalService renewalService;

    @GetMapping(value = "/api/v1/summoner/{puuid}/renewal-status")
    public ResponseEntity<SummonerRenewalStatus> checkSummonerRenewalStatus(
            @PathVariable("puuid") String puuid
    ) {



        return null;
    }
}
