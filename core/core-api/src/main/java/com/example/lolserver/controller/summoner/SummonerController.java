package com.example.lolserver.controller.summoner;

import com.example.lolserver.controller.summoner.response.SummonerAutoResponse;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.domain.summoner.dto.response.SummonerRenewalResponse;
import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerAutoDTO;
import com.example.lolserver.controller.summoner.response.SummonerResponse;
import com.example.lolserver.storage.redis.service.RedisService;
import com.example.lolserver.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerService;

    /**
     * 유저 검색 API
     * 유저명에서 태그명을 입력하지 않았을 때 다수의 유저가 검색될 수 있음
     *
     * @param q 유저명 (gameName, tagLine) or (gameName)
     * @param region 지역명
     * @return 유저 리스트
     */
    @GetMapping("/v1/summoners/search")
    public ResponseEntity<ApiResponse<List<SummonerResponse>>> searchSummoner(
            @RequestParam(name = "q", defaultValue = "hideonbush-kr1") String q,
            @RequestParam(name = "region", defaultValue = "kr", required = false) String region
    ) {
        List<SummonerResponse> allSummoner = summonerService.getAllSummoner(q, region);

        return ResponseEntity.ok(ApiResponse.success(allSummoner));
    }

    /**
     * 유저 상세 정보 API
     * @param region 지역명
     * @param gameName 유저 게임명
     * @return 유저 상세 정보
     */
    @GetMapping("/v1/summoners/{region}/{gameName}")
    public ResponseEntity<ApiResponse<SummonerResponse>> getSummoner(
            @PathVariable("region") String region,
            @PathVariable("gameName") String gameName
    ) {
        SummonerResponse summoner = summonerService.getSummoner(gameName, region);

        return ResponseEntity.ok(ApiResponse.success(summoner));
    }

    /**
     * 유저명 자동완성 API
     * @param q 유저명
     * @param region 지역명
     * @return 유저 리스트
     */
    @GetMapping("/v1/summoners/autocomplete")
    public ResponseEntity<ApiResponse<List<SummonerAutoResponse>>> autoComplete(
            @RequestParam String q,
            @RequestParam(defaultValue = "kr") String region
    ) {
        List<SummonerAutoDTO> result = summonerService.getAllSummonerAutoComplete(q, region);

        return ResponseEntity.ok(ApiResponse.success(
                result.stream().map(SummonerAutoResponse::of).toList()
        ));
    }

    /**
     * 유저 전적 갱신
     * @param platform
     * @param puuid
     * @return
     */
    @GetMapping("/summoners/renewal/{platform}/{puuid}")
    public ResponseEntity<ApiResponse<SummonerRenewalResponse>> renewalSummonerInfo(
            @PathVariable(name = "platform") String platform,
            @PathVariable("puuid") String puuid
    ) {
        SummonerRenewalResponse summonerRenewalResponse = summonerService.renewalSummonerInfo(platform, puuid);
        return ResponseEntity.ok(ApiResponse.success(summonerRenewalResponse));
    }

    /**
     * 유저 정보 갱신 상태 조회 API 
     * @param puuid 유저 ID
     * @return 유저 정보 갱신 상태
     */
    @GetMapping("/v1/summoners/{puuid}/renewal-status")
    public ResponseEntity<ApiResponse<SummonerRenewalResponse>> summonerRenewalStatus(
            @PathVariable String puuid
    ) {
        log.info("summonerRenewalStatus");
        SummonerRenewalResponse summonerRenewalResponse = summonerService.renewalSummonerStatus(puuid);
        log.info("summonerRenewalResponse {}", summonerRenewalResponse);

        return ResponseEntity.ok(ApiResponse.success(summonerRenewalResponse));
    }

}