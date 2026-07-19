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
        }

        ChampionRotate newChampionRotate = championClientPort.getChampionRotate(platformId);

        // 빈 로테이션(upstream 조회 실패)은 캐싱하지 않는다.
        // 캐싱하면 TTL 동안 빈 값이 고착되어, upstream 이 복구된 뒤에도 계속 빈 값을 서빙한다.
        if (newChampionRotate.isEmpty()) {
            log.warn("빈 챔피언 로테이션 응답 — 캐싱을 생략한다. platformId: {}", platformId);
            return newChampionRotate;
        }

        championPersistencePort.saveChampionRotate(platformId, newChampionRotate);
        return newChampionRotate;
    }
}
