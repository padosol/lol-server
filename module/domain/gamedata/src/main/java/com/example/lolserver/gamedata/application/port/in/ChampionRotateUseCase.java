package com.example.lolserver.gamedata.application.port.in;

import com.example.lolserver.gamedata.domain.ChampionRotate;

public interface ChampionRotateUseCase {
    ChampionRotate getChampionRotate(String platformId);
}
