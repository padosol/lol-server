package com.example.lolserver.domain.champion.service;

import com.example.lolserver.riot.dto.champion.ChampionInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChampionServiceV1 implements ChampionService{

    private final ChampionRotateReader championRotateReader;

    @Override
    public ChampionInfo getRotation(String region) {
        return championRotateReader.read(region);
    }
}
