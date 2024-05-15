package com.example.lolserver.web.summoner.service.api;

import com.example.lolserver.web.summoner.entity.Summoner;

public interface RSummonerService {
    Summoner getSummoner(String gameName, String tagLine, String region);
}
