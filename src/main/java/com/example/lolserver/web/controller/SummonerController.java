package com.example.lolserver.web.controller;

import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.service.summoner.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerService;

    @GetMapping("/v1/summoners/{summonerName}")
    public ResponseEntity<SearchData> searchSummoner(
            @PathVariable String summonerName
    ) throws IOException, InterruptedException {

        SearchData searchData = summonerService.findSummoner(summonerName);

        return new ResponseEntity<>(searchData, HttpStatus.OK);
    }


    @GetMapping("/v1/summoners/renewal")
    public ResponseEntity<Boolean> renewalSummonerInfo(
        @RequestParam("puuid") String puuid
    ) throws IOException, InterruptedException {
        boolean result = summonerService.renewalSummonerInfo(puuid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


}
