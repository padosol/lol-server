package com.example.lolserver.controller.v1;

import java.util.List;

import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerResponse;
import com.example.lolserver.storage.redis.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lolserver.domain.summoner.service.SummonerService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Summoner API")
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final RedisService redisService;
    private final SummonerService summonerService;

    /**
     * 유저 검색 API
     * 유저명에서 태그명을 입력하지 않았을 때 다수의 유저가 검색될 수 있음
     *
     * @param q 유저명 (gameName, tagLine) or (gameName)
     * @param region 지역명
     * @return 유저 리스트
     */
    @Operation(description = "유저 검색 API", summary = "유저 검색 API")
    @GetMapping("/v1/summoners/search")
    public ResponseEntity<List<SummonerResponse>> searchSummoner(
            @RequestParam(name = "q", defaultValue = "hideonbush-kr1") String q,
            @RequestParam(name = "region", defaultValue = "kr", required = false) String region
    ) {
        List<SummonerResponse> allSummoner = summonerService.getAllSummoner(q, region);

        return new ResponseEntity<>(allSummoner, HttpStatus.OK);
    }

    /**
     * 유저 상세 정보 API
     * @param region 지역명
     * @param gameName 유저 게임명
     * @return 유저 상세 정보
     */
    @GetMapping("/v1/summoners/{region}/{gameName}")
    public ResponseEntity<SummonerResponse> getAllSummoner(
            @PathVariable("region") String region,
            @PathVariable("gameName") String gameName
    ) {
        SummonerResponse summoner = summonerService.getSummoner(gameName, region);

        return new ResponseEntity<>(summoner, HttpStatus.OK);
    }

    /**
     * 유저명 자동완성 API
     * @param q 유저명
     * @param region 지역명
     * @return 유저 리스트
     */
    @GetMapping("/v1/summoners/autocomplete")
    public ResponseEntity<List<SummonerResponse>> autoComplete(
            @RequestParam String q,
            @RequestParam(defaultValue = "kr") String region
    ) {
        List<SummonerResponse> allSummonerAutoComplete = summonerService.getAllSummonerAutoComplete(q, region);

        return new ResponseEntity<>(allSummonerAutoComplete, HttpStatus.OK);
    }

    /**
     * 유저 정보 갱신 API   
     * @param puuid 유저 ID
     * @return 유저 정보
     */
    @GetMapping("/v1/summoners/renewal")
    public ResponseEntity<String> renewalSummonerInfo(
        @RequestParam String puuid
    ) {
        summonerService.renewalSummonerInfo("kr", puuid);
        return new ResponseEntity<>(puuid, HttpStatus.OK);
    }

    /**
     * 유저 정보 갱신 상태 조회 API 
     * @param puuid 유저 ID
     * @return 유저 정보 갱신 상태
     * @throws JsonProcessingException
     */
    @GetMapping("/v1/summoners/{puuid}/renewal-status")
    public ResponseEntity<Boolean> summonerRenewalStatus(
            @PathVariable String puuid
    ) throws JsonProcessingException {

        boolean status = redisService.summonerRenewalStatus(puuid);

        if(status) {
            return new ResponseEntity<>(true, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(false, HttpStatus.OK);
        }
    }

}