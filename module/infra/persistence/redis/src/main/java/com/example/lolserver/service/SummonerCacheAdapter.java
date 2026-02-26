package com.example.lolserver.service;

import com.example.lolserver.domain.summoner.application.port.out.SummonerCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummonerCacheAdapter implements SummonerCachePort {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "summoner:lock:";
    private static final String RENEWAL_PREFIX = "summoner:renewal:";
    private static final String CLICK_COOLDOWN_PREFIX = "summoner:click-cooldown:";
    private static final long LOCK_WAIT_TIME = 5L;
    private static final long LOCK_LEASE_TIME = 10L;

    @Override
    public boolean isUpdating(String puuid) {
        String s = stringRedisTemplate.opsForValue().get(RENEWAL_PREFIX + puuid);
        return StringUtils.hasText(s);
    }

    @Override
    public void createSummonerRenewal(String puuid) {
        stringRedisTemplate.opsForValue().set(RENEWAL_PREFIX + puuid, puuid, 2, TimeUnit.MINUTES);
    }

    @Override
    public boolean isSummonerRenewal(String puuid) {
        String key = RENEWAL_PREFIX + puuid;
        String summonerRenewal = stringRedisTemplate.opsForValue().get(key);
        boolean result = StringUtils.hasText(summonerRenewal);
        log.info("소환사 갱신 상태 조회 - key: {}, puuid: {}, value: {}, result: {}", key, puuid, summonerRenewal, result);
        return result;
    }

    @Override
    public boolean tryLock(String key) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            return lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Lock acquisition interrupted for key: {}", key, e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public Set<String> getRefreshingPuuids() {
        Set<String> puuids = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(RENEWAL_PREFIX + "*")
                .count(100)
                .build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                puuids.add(key.substring(RENEWAL_PREFIX.length()));
            }
        }
        return puuids;
    }

    @Override
    public boolean isClickCooldown(String puuid) {
        String value = stringRedisTemplate.opsForValue().get(CLICK_COOLDOWN_PREFIX + puuid);
        return StringUtils.hasText(value);
    }

    @Override
    public void setClickCooldown(String puuid) {
        stringRedisTemplate.opsForValue().set(CLICK_COOLDOWN_PREFIX + puuid, puuid, 10, TimeUnit.SECONDS);
    }
}
