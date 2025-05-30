package com.example.lolserver.redis.service;

import java.util.List;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.example.lolserver.redis.model.SummonerRankSession;
import com.example.lolserver.redis.model.SummonerRenewalSession;
import com.example.lolserver.redis.repository.SummonerRenewalRepository;
import com.example.lolserver.web.match.dto.LinePosition;
import com.example.lolserver.web.match.dto.MSChampionResponse;
import com.example.lolserver.web.match.repository.matchsummoner.dsl.MatchSummonerRepositoryCustom;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService{

    private final RedisTemplate<String, Object> redisTemplate;
    private final MatchSummonerRepositoryCustom matchSummonerRepositoryCustom;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SummonerRenewalRepository summonerRenewalRepository;


    @Override
    public void addRankData(SummonerRankSession session) {

        ZSetOperations<String, Object> rank = redisTemplate.opsForZSet();
        HashOperations<String, Object, Object> summoner = redisTemplate.opsForHash();

        List<LinePosition> position = matchSummonerRepositoryCustom.findAllPositionByPuuidAndLimit(session.getPuuid(), 1L);
        List<MSChampionResponse> mostChampions = matchSummonerRepositoryCustom.findAllChampionKDAByPuuidAndSeasonAndQueueType(session.getPuuid(), 23, session.getQueueType().getQueueId(), 3L);

        // 랭킹 정보 등록, sorted set
        // 유저 정보 등록, hash
        // 유저 모스트 챔피언, 유저 모스트 라인
        if(position.size() > 0) {
            session.setPosition(position.get(0).getPosition());
        }

        if(mostChampions.size() > 0) {
            List<String> result = mostChampions.stream().map(MSChampionResponse::getChampionName).toList();
            session.setChampionNames(result);
        }

        if(session.hasKey()) {
            rank.add("rank_" + session.getKey(), session.getSummonerName(), session.getScore());
            summoner.put("summoner_" + session.getKey(), session.getSummonerName(), session);
        }

    }

    @Override
    public boolean summonerRenewalStatus(String puuid){

        try {
            SummonerRenewalSession summonerRenewalSession = summonerRenewalRepository.findById(puuid).orElseThrow(IllegalStateException::new);

            summonerRenewalRepository.save(summonerRenewalSession);
            return true;
        } catch(Exception e) {
            return false;
        }

    }
}
