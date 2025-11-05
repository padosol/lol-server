package com.example.lolserver.web.summoner.service;

import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.dto.response.SummonerRenewalResponse;

import java.util.List;

public interface SummonerService {
    SummonerResponse getSummoner(String q, String region);

    List<SummonerResponse> getAllSummoner(String q, String region);

    List<SummonerResponse> getAllSummonerAutoComplete(String q, String region);

    SummonerRenewalResponse renewalSummonerInfo(String platform, String puuid);

}
