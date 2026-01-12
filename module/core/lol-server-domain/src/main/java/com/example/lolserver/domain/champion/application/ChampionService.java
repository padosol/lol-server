package com.example.lolserver.domain.champion.application;

import com.example.lolserver.domain.champion.application.port.in.ChampionRotateUseCase;
import com.example.lolserver.domain.champion.application.port.out.ChampionClientPort;
import com.example.lolserver.domain.champion.application.port.out.ChampionPersistencePort;
import com.example.lolserver.domain.champion.domain.ChampionRotate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChampionService implements ChampionRotateUseCase {

    private final ChampionClientPort championClientPort;
    private final ChampionPersistencePort championPersistencePort;

    @Override
    public ChampionRotate getChampionRotate(String region) {
        Optional<ChampionRotate> championRotate = championPersistencePort.getChampionRotate(region);
        if (championRotate.isPresent()) {
            return championRotate.get();
        } else {
            ChampionRotate newChampionRotate = championClientPort.getChampionRotate(region);
            championPersistencePort.saveChampionRotate(region, newChampionRotate);
            return newChampionRotate;
        }
    }
}
