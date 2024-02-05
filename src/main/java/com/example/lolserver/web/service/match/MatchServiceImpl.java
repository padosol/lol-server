package com.example.lolserver.web.service.match;

import com.example.lolserver.entity.match.MatchSummoner;
import com.example.lolserver.web.dto.data.GameData;
import com.example.lolserver.web.repository.MatchSummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService{

    private final MatchSummonerRepository matchSummonerRepository;

    @Override
    public GameData getMatches(String puuid) {

        List<MatchSummoner> matchSummonerList = matchSummonerRepository.findMatchSummonerByPuuid(puuid);


        System.out.println("test");



        return null;
    }
}
