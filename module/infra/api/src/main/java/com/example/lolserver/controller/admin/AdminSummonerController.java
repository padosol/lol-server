package com.example.lolserver.controller.admin;

import com.example.lolserver.controller.support.response.ApiResponse;
import com.example.lolserver.domain.summoner.application.port.in.SummonerQueryUseCase;
import com.example.lolserver.domain.summoner.application.model.SummonerRenewalInfoReadModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSummonerController {

    private final SummonerQueryUseCase summonerQueryUseCase;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/summoners/renewals")
    public ResponseEntity<ApiResponse<List<SummonerRenewalInfoReadModel>>> getRefreshingSummoners() {
        List<SummonerRenewalInfoReadModel> result = summonerQueryUseCase.getRefreshingSummoners();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
