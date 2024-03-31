package com.example.lolserver.web.summoner.service;

import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;

import java.io.IOException;
import java.util.List;

public interface SummonerService {
    SearchData findSummoner(String summonerName) throws IOException, InterruptedException;

    SummonerResponse getSummoner(Summoner summoner);

    List<SummonerResponse> getAllSummoner(Summoner summoner) throws IOException, InterruptedException;

    boolean renewalSummonerInfo(String puuid) throws IOException, InterruptedException;

}
