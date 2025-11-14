package com.example.lolserver.domain.summoner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummonerRenewalResponse {
    private String puuid;
    private RenewalStatus status;
}
