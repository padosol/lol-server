package com.example.lolserver.web.rank.service;

import com.example.lolserver.redis.model.SummonerRankSession;
import com.example.lolserver.web.rank.dto.RankResponse;
import com.example.lolserver.web.rank.dto.RankSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService{

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<RankResponse> getSummonerRank(RankSearchDto rankSearchDto) {

        ZSetOperations<String, Object> rank = redisTemplate.opsForZSet();
        HashOperations<String, Object, Object> summoner = redisTemplate.opsForHash();

        List<RankResponse> result = new ArrayList<>();

        int blockSize = 20;
        int start = blockSize * (rankSearchDto.getPage() - 1);

        Set<Object> range = rank.range("rank_" + rankSearchDto.getType().getKey(), start, start + blockSize - 1);

        assert range != null;
        for (Object summonerData : range) {
            String summonerId = (String) summonerData;

            SummonerRankSession summonerRankSession = (SummonerRankSession)summoner.get("summoner_" + rankSearchDto.getType().getKey(), summonerId);

            assert summonerRankSession != null;

            result.add(new RankResponse(summonerRankSession));
        }

        return result;
    }
}
