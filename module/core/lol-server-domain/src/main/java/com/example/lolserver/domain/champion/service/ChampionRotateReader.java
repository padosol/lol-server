package com.example.lolserver.domain.champion.service;

import com.example.lolserver.riot.client.summoner.ChampionRotateRestClient;
import com.example.lolserver.riot.dto.champion.ChampionInfo;
import com.example.lolserver.storage.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChampionRotateReader {

    private final RedisService redisService;
    private final ChampionRotateRestClient championRotateRestClient;
    // 서버 캐시
    // 레디스 캐시
    // DB 캐시
    // 외부 API 호출
    public ChampionInfo read(String region) {
        return championRotateRestClient.getChampionInfo(region);
    }

}
