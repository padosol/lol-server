package com.example.lolserver.domain.champion.application.port.out;

import com.example.lolserver.domain.champion.domain.ChampionRotate;

public interface ChampionClientPort {
    ChampionRotate getChampionRotate(String region);
}
