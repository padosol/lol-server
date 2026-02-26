package com.example.lolserver.domain.champion.application;

import com.example.lolserver.domain.champion.application.port.in.ChampionRotateUseCase;
import com.example.lolserver.domain.champion.application.port.out.ChampionClientPort;
import com.example.lolserver.domain.champion.application.port.out.ChampionPersistencePort;
import com.example.lolserver.domain.champion.domain.ChampionRotate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
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
            championPersistencePort.saveChampionRotate(platformId, newChampionRotate);
            return newChampionRotate;
        }
    }
}
