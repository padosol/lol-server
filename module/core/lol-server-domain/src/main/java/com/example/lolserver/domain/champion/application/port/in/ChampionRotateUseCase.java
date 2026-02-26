package com.example.lolserver.domain.champion.application.port.in;

import com.example.lolserver.domain.champion.domain.ChampionRotate;

public interface ChampionRotateUseCase {
    ChampionRotate getChampionRotate(String platformId);
}
