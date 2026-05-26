package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.domain.ChampionRotate;
import java.util.Optional;

public interface ChampionPersistencePort {
    Optional<ChampionRotate> getChampionRotate(String platformId);
    void saveChampionRotate(String platformId, ChampionRotate championRotate);
}
