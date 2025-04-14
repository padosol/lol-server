package com.example.lolserver.domain.summoner.application.api;

import com.example.lolserver.domain.summoner.domain.entity.Summoner;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.concurrent.ExecutionException;

public interface RSummonerService {
    Summoner getSummoner(String gameName, String tagLine, String region);
    Summoner getSummonerV2(String gameName, String tagLine, String region);

    Summoner fetchSummonerAllInfo(String gameName, String tagLine, String region);

    boolean revisionSummoner(String puuid);

    Summoner revisionSummonerV2(String puuid) throws ExecutionException, InterruptedException, JsonProcessingException;
}
