package com.example.lolserver.controller.summoner;

import com.example.lolserver.domain.summoner.application.model.SummonerAutoReadModel;
import com.example.lolserver.controller.summoner.response.SummonerRenewalResponse;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.domain.summoner.domain.SummonerRenewal;
import com.example.lolserver.domain.summoner.application.model.SummonerReadModel;
import com.example.lolserver.controller.support.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * @param platformId 플랫폼 ID
     * @param gameName 유저 게임명
     * @return 유저 상세 정보
     */
    @GetMapping("/v1/summoners/{platformId}/{gameName}")
    public ResponseEntity<ApiResponse<SummonerReadModel>> getSummoner(
            @PathVariable("platformId") String platformId,
            @PathVariable("gameName") String gameName
    ) {
        log.info("getSummoner");
        SummonerReadModel summoner = summonerService.getSummoner(GameName.create(gameName), platformId);

        return ResponseEntity.ok(ApiResponse.success(summoner));
    }

    /**
     * 유저 상세 정보
     * @param platformId 플랫폼 ID
     * @param puuid 소환사 PUUID
     * @return 유저 상세 정보
     */
    @GetMapping("/v1/{platformId}/summoners/{puuid}")
    public ResponseEntity<ApiResponse<SummonerReadModel>> getSummonerByPuuid(
            @PathVariable("platformId") String platformId,
            @PathVariable("puuid") String puuid
    ) {
        SummonerReadModel summonerResponse = summonerService.getSummonerByPuuid(platformId, puuid);

        return ResponseEntity.ok(ApiResponse.success(summonerResponse));
    }

    /**
     * 유저명 자동완성 API
     * @param platformId 플랫폼 ID
     * @param q 유저명
     * @return 유저 리스트
     */
    @GetMapping("/v1/{platformId}/summoners/autocomplete")
    public ResponseEntity<ApiResponse<List<SummonerAutoReadModel>>> autoComplete(
            @PathVariable("platformId") String platformId,
            @RequestParam String q
    ) {
        List<SummonerAutoReadModel> result = summonerService.getAllSummonerAutoComplete(q, platformId);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 소환사 전적 갱신 API
     *
     * <p>갱신 요청 후 즉시 응답하며, 실제 데이터 갱신은 비동기로 처리된다.
     * 클라이언트는 {@code /v1/summoners/{puuid}/renewal-status}를 폴링하여 완료를 확인한다.
     *
     * @param platformId 플랫폼 ID (예: "kr")
     * @param puuid    소환사 PUUID
     * @return 갱신 상태 (puuid, status)
     */
    @GetMapping("/v1/{platformId}/summoners/{puuid}/renewal")
    public ResponseEntity<ApiResponse<SummonerRenewalResponse>> renewalSummonerInfo(
            @PathVariable("platformId") String platformId,
            @PathVariable("puuid") String puuid
    ) {
        SummonerRenewal summonerRenewal = summonerService.renewalSummonerInfo(platformId, puuid);
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
        SummonerRenewal summonerRenewal = summonerService.renewalSummonerStatus(puuid);
        return ResponseEntity.ok(ApiResponse.success(
                new SummonerRenewalResponse(
                        summonerRenewal.getPuuid(), summonerRenewal.getStatus().name()
                )
        ));
    }
}
