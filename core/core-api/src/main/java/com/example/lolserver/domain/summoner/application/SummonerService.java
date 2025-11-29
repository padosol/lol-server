package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerResponse;
import com.example.lolserver.domain.summoner.dto.response.SummonerRenewalResponse;

import java.util.List;

public interface SummonerService {

    SummonerResponse getSummoner(String q, String region);

    SummonerResponse getSummoner(String puuid);

    List<SummonerResponse> getAllSummoner(String q, String region);

    List<SummonerResponse> getAllSummonerAutoComplete(String q, String region);

    SummonerRenewalResponse renewalSummonerInfo(String platform, String puuid);

    SummonerRenewalResponse renewalSummonerStatus(String puuid);

}
