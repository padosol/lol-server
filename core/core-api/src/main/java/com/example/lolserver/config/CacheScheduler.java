package com.example.lolserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheScheduler {

    @Scheduled(cron = "0 10 0 * * 2")
    @CacheEvict(value = "rotation", allEntries = true)
    public void evictRotationCache() {
        log.info("Rotation cache has been evicted.");
    }
}
