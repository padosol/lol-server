package com.example.lolserver.web.champion.service;

import com.example.lolserver.riot.dto.champion.ChampionInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ChampionServiceV1 implements ChampionService{
    @Override
    public ChampionInfo getRotation(String region) throws IOException, InterruptedException {


        return null;
    }
}
