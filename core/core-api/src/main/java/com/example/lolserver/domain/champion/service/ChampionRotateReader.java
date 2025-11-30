package com.example.lolserver.domain.champion.service;

import com.example.lolserver.riot.dto.champion.ChampionInfo;
import com.example.lolserver.storage.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChampionRotateReader {

    private final RedisService redisService;
    // 서버 캐시
    // 레디스 캐시
    // DB 캐시
    // 외부 API 호출
    public ChampionInfo read(String region) {

        return null;
    }

}
