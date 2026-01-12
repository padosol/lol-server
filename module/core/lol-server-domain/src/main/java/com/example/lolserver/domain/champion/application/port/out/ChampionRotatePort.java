package com.example.lolserver.domain.champion.application.port.out;

import com.example.lolserver.domain.champion.domain.ChampionRotate;
import java.util.Optional;

public interface ChampionRotatePort {
    Optional<ChampionRotate> getChampionRotate();
    void saveChampionRotate(ChampionRotate championRotate);
}
