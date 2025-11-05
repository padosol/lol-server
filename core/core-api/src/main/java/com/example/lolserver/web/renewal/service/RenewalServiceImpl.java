package com.example.lolserver.web.renewal.service;

import com.example.lolserver.web.renewal.dto.response.SummonerRenewalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RenewalServiceImpl implements RenewalService{

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public SummonerRenewalStatus checkRenewalStatus(String puuid) {


        // 없을경우 성공으로 간주함





        return null;
    }
}
