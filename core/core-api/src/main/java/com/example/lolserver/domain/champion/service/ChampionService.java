package com.example.lolserver.domain.champion.service;

import com.example.lolserver.riot.dto.champion.ChampionInfo;

public interface ChampionService {

    ChampionInfo getRotation(String region);

}
