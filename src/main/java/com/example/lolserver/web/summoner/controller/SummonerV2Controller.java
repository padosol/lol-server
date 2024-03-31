package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.service.SummonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

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

        List<SummonerResponse> result = summonerServiceV2.getAllSummoner(Summoner.builder()
                .name(q)
                .region(region)
                .build());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
