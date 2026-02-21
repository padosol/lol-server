package com.example.lolserver.controller.admin;

import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.domain.summoner.application.dto.SummonerRenewalInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSummonerController {

    private final SummonerService summonerService;

    @GetMapping("/summoners/renewals")
    public ResponseEntity<ApiResponse<List<SummonerRenewalInfoResponse>>> getRefreshingSummoners() {
        List<SummonerRenewalInfoResponse> result = summonerService.getRefreshingSummoners();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
