package com.example.lolserver.web.summoner.service;

import com.example.lolserver.web.summoner.dto.SummonerRequest;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;

import java.io.IOException;
import java.util.List;

public interface SummonerService {
    SummonerResponse getSummoner(String q, String region);

    List<SummonerResponse> getAllSummoner(String q, String region);

    boolean renewalSummonerInfo(String puuid) throws IOException, InterruptedException;

}
