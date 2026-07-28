package com.example.lolserver.gamedata.application;

import com.example.lolserver.gamedata.application.port.in.ChampionRotateUseCase;
import com.example.lolserver.gamedata.application.port.out.ChampionClientPort;
import com.example.lolserver.gamedata.application.port.out.ChampionPersistencePort;
import com.example.lolserver.gamedata.domain.ChampionRotate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChampionService implements ChampionRotateUseCase {

    private final ChampionClientPort championClientPort;
    private final ChampionPersistencePort championPersistencePort;

    @Override
    public ChampionRotate getChampionRotate(String platformId) {
        Optional<ChampionRotate> championRotate = championPersistencePort.getChampionRotate(platformId);
        if (championRotate.isPresent()) {
            return championRotate.get();
        } else {
            ChampionRotate newChampionRotate = championClientPort.getChampionRotate(platformId);
            // 빈 로테이션이어도 캐싱한다. 캐시 어댑터가 빈 값에는 짧은 TTL(negative cache)을 적용해
            // Riot rate limit 소모를 막으면서 upstream 복구를 빠르게 반영한다.
            championPersistencePort.saveChampionRotate(platformId, newChampionRotate);
            return newChampionRotate;
        }
    }
}
