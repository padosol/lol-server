package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.storage.db.core.repository.summoner.dto.SummonerAutoDTO;
import com.example.lolserver.controller.summoner.response.SummonerResponse;
import com.example.lolserver.domain.summoner.dto.response.SummonerRenewalResponse;

import java.util.List;

public interface SummonerService {

    SummonerResponse getSummoner(String q, String region);

    List<SummonerResponse> getAllSummoner(String q, String region);

    List<SummonerAutoDTO> getAllSummonerAutoComplete(String q, String region);

    SummonerRenewalResponse renewalSummonerInfo(String platform, String puuid);

    SummonerRenewalResponse renewalSummonerStatus(String puuid);

    SummonerResponse getSummonerByPuuid(String region, String puuid);

}
