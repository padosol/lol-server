package com.example.lolserver.gamedata.application.port.out;

import com.example.lolserver.gamedata.domain.ChampionRotate;

public interface ChampionClientPort {
    ChampionRotate getChampionRotate(String platformId);
}
