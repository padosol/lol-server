package com.example.lolserver.web.summoner.service;

import com.example.lolserver.web.summoner.dto.SummonerResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface SummonerService {
    SummonerResponse getSummoner(String q, String region);

    List<SummonerResponse> getAllSummoner(String q, String region);

    List<SummonerResponse> getAllSummonerAutoComplete(String q, String region);

    SummonerResponse renewalSummonerInfo(String puuid) throws IOException, InterruptedException, ExecutionException;

}
