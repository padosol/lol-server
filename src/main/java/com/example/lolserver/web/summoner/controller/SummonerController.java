package com.example.lolserver.web.summoner.controller;

import com.example.lolserver.web.summoner.dto.SummonerRequest;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.service.SummonerService;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerService;
    private final Bucket bucket;

    @GetMapping("/v1/summoners/search")
    public ResponseEntity<List<SummonerResponse>> searchSummoner(
            @RequestParam(value = "q") String q,
            @RequestParam(value = "region", required = false) String region
    ) {

        List<SummonerResponse> allSummoner = summonerService.getAllSummoner(q, region);

        return new ResponseEntity<>(allSummoner, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners/{region}/{gameName}")
    public ResponseEntity<SummonerResponse> getAllSummoner(
            @PathVariable(value = "region") String region,
            @PathVariable(value = "gameName") String gameName
    ) {
        SummonerResponse summoner = summonerService.getSummoner(gameName, region);

        return new ResponseEntity<>(summoner, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners/renewal")
    public ResponseEntity<Boolean> renewalSummonerInfo(
        @RequestParam("puuid") String puuid
    ) throws IOException, InterruptedException {

        // 안정적으로 10개 이상일때만 전적 갱신 가능 하도록 함 0 개로 설정하면 너무 타이트함
        log.info("사용가능한 Bucket 수: {}", bucket.getAvailableTokens());

        if(bucket.getAvailableTokens() > 10) {
            boolean result = summonerService.renewalSummonerInfo(puuid);

            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {

            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }

    }

}