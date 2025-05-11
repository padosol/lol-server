package com.example.lolserver.web.summoner.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummonerRenewalResponse {
    @Schema(description = "유저 puuid")
    private String puuid;
    @Schema(description = "갱신 상태")
    private RenewalStatus status;
}
