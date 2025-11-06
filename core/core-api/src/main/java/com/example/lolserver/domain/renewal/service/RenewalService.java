package com.example.lolserver.domain.renewal.service;

import com.example.lolserver.domain.renewal.dto.response.SummonerRenewalStatus;

public interface RenewalService {

    SummonerRenewalStatus checkRenewalStatus(String puuid);

}
