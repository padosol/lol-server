package com.example.lolserver.web.service.summoner;

import com.example.lolserver.web.dto.SearchData;

import java.io.IOException;

public interface SummonerService {
    SearchData findSummoner(String summonerName) throws IOException, InterruptedException;
}
