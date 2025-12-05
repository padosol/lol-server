package com.example.lolserver.domain.champion.service;

import com.example.lolserver.riot.dto.champion.ChampionInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChampionService{

    private final ChampionRotateReader championRotateReader;

    @Cacheable(value = "rotation", key = "#region")
    public ChampionInfo getRotation(String region) {
        return championRotateReader.read(region);
    }
}
