package com.example.lolserver.domain.summoner.application;

import com.example.lolserver.domain.summoner.api.dto.SummonerResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface SummonerService {
    SummonerResponse getSummoner(String q, String region);

    List<SummonerResponse> getAllSummoner(String q, String region);

    List<SummonerResponse> getAllSummonerAutoComplete(String q, String region);

    SummonerResponse renewalSummonerInfo(String puuid) throws IOException, InterruptedException, ExecutionException;

}
