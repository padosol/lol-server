package com.example.lolserver.web.service.summoner;

import com.example.lolserver.entity.summoner.Summoner;
import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.dto.data.SummonerData;
import com.example.lolserver.web.dto.response.SummonerResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface SummonerService {
    SearchData findSummoner(String summonerName) throws IOException, InterruptedException;

    List<SummonerData> getAllSummoner(Summoner summoner) throws IOException, InterruptedException;

    boolean renewalSummonerInfo(String puuid) throws IOException, InterruptedException;

    List<SummonerData> getSummoners(String encodeSummonerName) throws UnsupportedEncodingException;


    // v2
    SummonerResponse findSummonerV2(Summoner summoner) throws IOException, InterruptedException;

}
