package com.example.lolserver.web.service;

import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.dto.data.SummonerData;

import java.io.IOException;

public interface LolService {
    SearchData findSummoner(String summonerName) throws IOException, InterruptedException;
}
