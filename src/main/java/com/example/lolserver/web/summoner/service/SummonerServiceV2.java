package com.example.lolserver.web.summoner.service;

import com.example.lolserver.web.dto.SearchData;
import com.example.lolserver.web.summoner.dto.SummonerResponse;
import com.example.lolserver.web.summoner.entity.Summoner;
import com.example.lolserver.web.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service("summonerServiceV2")
@RequiredArgsConstructor
public class SummonerServiceV2 implements SummonerService{

    private final SummonerRepository summonerRepository;

    @Override
    public SearchData findSummoner(String summonerName) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public SummonerResponse getSummoner(Summoner summoner) {
        return null;
    }

    @Override
    public List<SummonerResponse> getAllSummoner(Summoner summoner) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public boolean renewalSummonerInfo(String puuid) throws IOException, InterruptedException {



        return false;
    }

}
