package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerV2Controller {

    private final SummonerService summonerServiceV2;

    @GetMapping("/v2/summoners/{summonerName}")
    public ResponseEntity<SearchData> searchSummoner(
            @PathVariable String summonerName
    ) {

        return null;
    }

    @GetMapping("/v2/summoners")
    public ResponseEntity<List<SummonerResponse>> getAllSummoner(
            @RequestParam("q") String q, @RequestParam("region") String region
    ) throws IOException, InterruptedException {

        log.info("getAllSummoner start");

        List<SummonerResponse> result = summonerServiceV2.getAllSummoner(Summoner.builder()
                .name(q)
                .region(region)
                .build());

        for (SummonerResponse summonerResponse : result) {
            log.info("Gamename: {}", summonerResponse.getGameName());
            log.info("Tagline: {}", summonerResponse.getTagLine());
        }



        log.info("getAllSummoner end");

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @GetMapping("/v2/summoners/renewal")
    public ResponseEntity<Boolean> renewalSummonerInfo(
            @RequestParam("puuid") String puuid
    ) throws IOException, InterruptedException {
        boolean result = summonerServiceV2.renewalSummonerInfo(puuid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
