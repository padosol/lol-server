package com.example.lolserver.domain.rank.service;

import com.example.lolserver.domain.rank.dto.RankResponse;
import com.example.lolserver.domain.rank.dto.RankSearchDto;
import com.example.lolserver.model.SummonerRankSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RankService{

    private final RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> getSummonerRank(RankSearchDto rankSearchDto) {

        Map<String, Object> data = new HashMap<>();

        ZSetOperations<String, Object> rank = redisTemplate.opsForZSet();
        HashOperations<String, Object, Object> summoner = redisTemplate.opsForHash();

        List<RankResponse> result = new ArrayList<>();

        int blockSize = 20;
        int start = blockSize * (rankSearchDto.getPage() - 1);

        String key = "rank_" + rankSearchDto.getType().getKey();

        Set<Object> range = rank.range(key, start, start + blockSize - 1);

        Long total = rank.zCard(key);
        Long totalPage = (total / 20) + 1;

        assert range != null;
        for (Object summonerData : range) {
            String summonerId = (String) summonerData;

            SummonerRankSession summonerRankSession = (SummonerRankSession)summoner.get("summoner_" + rankSearchDto.getType().getKey(), summonerId);

            assert summonerRankSession != null;

            result.add(new RankResponse(summonerRankSession));
        }

        data.put("result", result);
        data.put("total", total);
        data.put("totalPage", totalPage);

        return data;
    }
}
