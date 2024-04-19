package com.example.lolserver.web.champion.service;

import com.example.lolserver.riot.dto.champion.ChampionInfo;

import java.io.IOException;

public interface ChampionService {

    ChampionInfo getRotation(String region) throws IOException, InterruptedException;

}
