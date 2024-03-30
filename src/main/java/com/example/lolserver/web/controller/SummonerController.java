package com.example.lolserver.web.controller;

import com.example.lolserver.entity.summoner.Summoner;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.dto.data.SummonerData;
import com.example.lolserver.web.dto.response.SummonerResponse;
import com.example.lolserver.web.service.summoner.SummonerService;
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

    private final SummonerService summonerService;

    @GetMapping("/v1/summoners/{summonerName}")
    public ResponseEntity<SearchData> searchSummonerV1(
            @PathVariable String summonerName
    ) throws IOException, InterruptedException {

        SearchData result = summonerService.findSummoner(summonerName);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/v2/summoners/{summonerName}")
    public ResponseEntity<SearchData> searchSummonerV2(
            @PathVariable String summonerName
    ) {


        return null;
    }

    @GetMapping("/v1/summoners")
    public ResponseEntity<List<SummonerData>> getAllSummoner(
            @RequestParam("q") String q
    ) throws IOException, InterruptedException {

        List<SummonerData> result = summonerService.getAllSummoner(Summoner.builder()
                .name(q)
                .build());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners/renewal")
    public ResponseEntity<Boolean> renewalSummonerInfo(
        @RequestParam("puuid") String puuid
    ) throws IOException, InterruptedException {
        boolean result = summonerService.renewalSummonerInfo(puuid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/v1/summoner/by-name/{summonerName}")
    public ResponseEntity<List<SummonerData>> getSummoners(
            @PathVariable String summonerName
    ) throws UnsupportedEncodingException {

        List<SummonerData> result = summonerService.getSummoners(summonerName);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


}
