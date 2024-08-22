package com.example.lolserver.web.renewal.service;

import com.example.lolserver.web.renewal.dto.response.SummonerRenewalStatus;

public interface RenewalService {

    SummonerRenewalStatus checkRenewalStatus(String puuid);

}
