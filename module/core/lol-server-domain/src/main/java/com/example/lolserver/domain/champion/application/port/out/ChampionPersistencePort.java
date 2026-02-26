package com.example.lolserver.domain.champion.application.port.out;

import com.example.lolserver.domain.champion.domain.ChampionRotate;
import java.util.Optional;

public interface ChampionPersistencePort {
    Optional<ChampionRotate> getChampionRotate(String platformId);
    void saveChampionRotate(String platformId, ChampionRotate championRotate);
}
