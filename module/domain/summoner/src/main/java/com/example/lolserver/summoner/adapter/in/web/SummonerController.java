package com.example.lolserver.summoner.adapter.in.web;

import com.example.lolserver.summoner.application.model.readmodel.SummonerAutoReadModel;
import com.example.lolserver.summoner.adapter.in.web.response.SummonerRenewalResponse;
import com.example.lolserver.summoner.application.port.in.SummonerQueryUseCase;
import com.example.lolserver.summoner.application.port.in.SummonerUseCase;
import com.example.lolserver.summoner.domain.SummonerRenewal;
import com.example.lolserver.summoner.application.model.readmodel.SummonerReadModel;
import com.example.lolserver.common.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.example.lolserver.summoner.domain.vo.GameName;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummonerController {

    private final SummonerQueryUseCase summonerQueryUseCase;
    private final SummonerUseCase summonerUseCase;

    /**
     * Riot ID 로 소환사 조회
     * @param gameName 유저 게임명 (gameName-tagLine)
     * @param platform 플랫폼 ID (예: kr)
     */
    @GetMapping("/v1/summoners/by-riot-id/{gameName}")
    public ResponseEntity<ApiResponse<SummonerReadModel>> getSummonerByRiotId(
            @PathVariable("gameName") String gameName,
            @RequestParam String platform
    ) {
        SummonerReadModel summoner = summonerQueryUseCase.getSummoner(GameName.create(gameName), platform);
        return ResponseEntity.ok(ApiResponse.success(summoner));
    }

    /**
     * puuid 로 저장된 소환사 조회. 플랫폼은 저장된 값을 쓴다.
     */
    @GetMapping("/v1/summoners/{puuid}")
    public ResponseEntity<ApiResponse<SummonerReadModel>> getSummonerByPuuid(
            @PathVariable("puuid") String puuid
    ) {
        return ResponseEntity.ok(ApiResponse.success(summonerQueryUseCase.getSummonerByPuuid(puuid)));
    }

    @GetMapping("/v1/summoners/autocomplete")
    public ResponseEntity<ApiResponse<List<SummonerAutoReadModel>>> autoCompleteByPlatform(
            @RequestParam String q,
            @RequestParam String platform
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                summonerQueryUseCase.getAllSummonerAutoComplete(q, platform)));
    }

    /**
     * 소환사 전적 갱신 요청. 실제 갱신은 비동기로 처리되므로 202 를 반환하고,
     * 클라이언트는 {@code GET /v1/summoners/{puuid}/renewal} 로 상태를 폴링한다.
     */
    @PostMapping("/v1/summoners/{puuid}/renewal")
    public ResponseEntity<ApiResponse<SummonerRenewalResponse>> requestRenewal(
            @PathVariable("puuid") String puuid
    ) {
        SummonerRenewal summonerRenewal = summonerUseCase.renewalSummonerInfo(puuid);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                new SummonerRenewalResponse(
                        summonerRenewal.getPuuid(), summonerRenewal.getStatus().name()
                )
        ));
    }

    @GetMapping("/v1/summoners/{puuid}/renewal")
    public ResponseEntity<ApiResponse<SummonerRenewalResponse>> getRenewalStatus(
            @PathVariable("puuid") String puuid
    ) {
        return summonerRenewalStatus(puuid);
    }

    // ---- legacy: lol-ui 전환 후 MP-156 에서 제거 ----

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
        SummonerReadModel summoner = summonerQueryUseCase.getSummoner(GameName.create(gameName), platformId);

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
        SummonerReadModel summonerResponse = summonerQueryUseCase.getSummonerByPuuid(platformId, puuid);

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
        List<SummonerAutoReadModel> result = summonerQueryUseCase.getAllSummonerAutoComplete(q, platformId);

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
        SummonerRenewal summonerRenewal = summonerUseCase.renewalSummonerInfo(platformId, puuid);
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
        SummonerRenewal summonerRenewal = summonerQueryUseCase.renewalSummonerStatus(puuid);
        return ResponseEntity.ok(ApiResponse.success(
                new SummonerRenewalResponse(
                        summonerRenewal.getPuuid(), summonerRenewal.getStatus().name()
                )
        ));
    }
}
