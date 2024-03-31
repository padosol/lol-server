package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerServiceV1;

    @GetMapping("/v1/summoners/{summonerName}")
    public ResponseEntity<SearchData> searchSummonerV1(
            @PathVariable String summonerName
    ) throws IOException, InterruptedException {

        SearchData result = summonerServiceV1.findSummoner(summonerName);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners")
    public ResponseEntity<List<SummonerResponse>> getAllSummoner(
            @RequestParam("q") String q
    ) throws IOException, InterruptedException {

        List<SummonerResponse> result = summonerServiceV1.getAllSummoner(Summoner.builder()
                .name(q)
                .build());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners/renewal")
    public ResponseEntity<Boolean> renewalSummonerInfo(
        @RequestParam("puuid") String puuid
    ) throws IOException, InterruptedException {
        boolean result = summonerServiceV1.renewalSummonerInfo(puuid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
