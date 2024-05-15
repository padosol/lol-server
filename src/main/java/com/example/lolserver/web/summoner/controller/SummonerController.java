package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.web.summoner.dto.SummonerRequest;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerService;

    @GetMapping("/v1/summoners/search")
    public ResponseEntity<List<SummonerResponse>> searchSummoner(
            @RequestParam(value = "q") String q,
            @RequestParam(value = "region", required = false) String region
    ) {

        List<SummonerResponse> allSummoner = summonerService.getAllSummoner(q, region);

        return new ResponseEntity<>(allSummoner, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners/{region}/{gameName}")
    public ResponseEntity<List<SummonerResponse>> getAllSummoner(
            @PathVariable(value = "region") String region,
            @PathVariable(value = "gameName") String gameName
    ) throws IOException, InterruptedException {

        List<SummonerResponse> result = summonerService.getAllSummoner(region ,gameName);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners/renewal")
    public ResponseEntity<Boolean> renewalSummonerInfo(
        @RequestParam("puuid") String puuid
    ) throws IOException, InterruptedException {
        boolean result = summonerService.renewalSummonerInfo(puuid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}