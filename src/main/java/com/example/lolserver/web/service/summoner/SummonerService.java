package com.example.lolserver.web.service.summoner;

import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.dto.response.SummonerResponse;

import java.io.IOException;
import java.util.List;

public interface SummonerService {
    SearchData findSummoner(String summonerName) throws IOException, InterruptedException;

    boolean renewalSummonerInfo(String puuid) throws IOException, InterruptedException;

    List<SummonerResponse> getSummoners(String summonerName);
}
