package com.example.lolserver.domain.summoner.api;

import com.example.lolserver.domain.summoner.api.dto.SummonerResponse;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.redis.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final RedisService redisService;
    private final SummonerService summonerService;
    private final Bucket bucket;

    /**
     * 유저 검색 API
     * @param q 유저명
     * @param region 지역명
     * @return 유저 리스트
     */
    @GetMapping("/v1/summoners/search")
    public ResponseEntity<List<SummonerResponse>> searchSummoner(
            @RequestParam(value = "q") String q,
            @RequestParam(value = "region", required = false) String region
    ) {

        List<SummonerResponse> allSummoner = summonerService.getAllSummoner(q, region);

        return new ResponseEntity<>(allSummoner, HttpStatus.OK);
    }

    /**
     * 유저명 자동완성 API
     * @param q 유저명
     * @param region 지역명
     * @return 유저 리스트
     */
    @GetMapping("/v1/summoners/autocomplete")
    public ResponseEntity<List<SummonerResponse>> autoComplete(
            @RequestParam(value = "q") String q,
            @RequestParam(value = "region", required = false) String region
    ) {
        List<SummonerResponse> allSummonerAutoComplete = summonerService.getAllSummonerAutoComplete(q, region);

        return new ResponseEntity<>(allSummonerAutoComplete, HttpStatus.OK);
    }

    /**
     * 유저 상세 정보 API
     * @param region 지역명
     * @param gameName 유저 게임명
     * @return 유저 상세 정보
     */
    @GetMapping("/v1/summoners/{region}/{gameName}")
    public ResponseEntity<SummonerResponse> getAllSummoner(
            @PathVariable(value = "region") String region,
            @PathVariable(value = "gameName") String gameName
    ) {
        SummonerResponse summoner = summonerService.getSummoner(gameName, region);

        return new ResponseEntity<>(summoner, HttpStatus.OK);
    }

    @GetMapping("/v1/summoners/renewal")
    public ResponseEntity<SummonerResponse> renewalSummonerInfo(
        @RequestParam("puuid") String puuid
    ) throws IOException, InterruptedException, ExecutionException {

        if(bucket.getAvailableTokens() > 10) {
            SummonerResponse result = summonerService.renewalSummonerInfo(puuid);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @GetMapping("/v1/summoners/{puuid}/renewal-status")
    public ResponseEntity<Boolean> summonerRenewalStatus(
            @PathVariable("puuid") String puuid
    ) throws JsonProcessingException {

        boolean status = redisService.summonerRenewalStatus(puuid);

        if(status) {
            return new ResponseEntity<>(true, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(false, HttpStatus.OK);
        }
    }

}