package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.domain.ChampionRotate;
import java.util.Optional;

public interface ChampionRotatePort {
    Optional<ChampionRotate> getChampionRotate();
    void saveChampionRotate(ChampionRotate championRotate);
}
