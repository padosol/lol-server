package com.example.lolserver.controller.summoner;

import com.example.lolserver.domain.summoner.application.dto.SummonerAutoResponse;
import com.example.lolserver.controller.summoner.response.SummonerRenewalResponse;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.domain.summoner.domain.SummonerRenewal;
import com.example.lolserver.domain.summoner.application.dto.SummonerResponse;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.lolserver.domain.summoner.domain.vo.GameName;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerService summonerService;

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
        log.info("getSummoner");
        SummonerResponse summoner = summonerService.getSummoner(GameName.create(gameName), region);

        return ResponseEntity.ok(ApiResponse.success(summoner));
    }

    /**
     * 유저 상세 정보
     * @param region
     * @param puuid
     * @return
     */
    @GetMapping("/v1/{region}/summoners/{puuid}")
    public ResponseEntity<ApiResponse<SummonerResponse>> getSummonerByPuuid(
            @PathVariable("region") String region,
            @PathVariable("puuid") String puuid
    ) {
        SummonerResponse summonerResponse = summonerService.getSummonerByPuuid(region, puuid);

        return ResponseEntity.ok(ApiResponse.success(summonerResponse));
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
        List<SummonerAutoResponse> result = summonerService.getAllSummonerAutoComplete(q, region);

        return ResponseEntity.ok(ApiResponse.success(result));
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
        log.info("API 호출 ");
        SummonerRenewal summonerRenewal = summonerService.renewalSummonerInfo(platform, puuid);
        return ResponseEntity.ok(ApiResponse.success(
                new SummonerRenewalResponse(
                        summonerRenewal.getPuuid(), summonerRenewal.getStatus().name()
                )
        ));
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
        SummonerRenewal summonerRenewal = summonerService.renewalSummonerStatus(puuid);
        return ResponseEntity.ok(ApiResponse.success(
                new SummonerRenewalResponse(
                        summonerRenewal.getPuuid(), summonerRenewal.getStatus().name()
                )
        ));
    }
}
