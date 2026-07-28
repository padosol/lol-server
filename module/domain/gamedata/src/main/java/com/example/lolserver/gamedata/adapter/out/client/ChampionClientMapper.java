package com.example.lolserver.gamedata.adapter.out.client;

import com.example.lolserver.gamedata.domain.ChampionRotate;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ChampionClientMapper {

    ChampionClientMapper INSTANCE = Mappers.getMapper(ChampionClientMapper.class);

    default ChampionRotate toDomain(ChampionInfo championInfo) {
        return new ChampionRotate(
                championInfo.maxNewPlayerLevel(),
                championInfo.freeChampionIdsForNewPlayers(),
                championInfo.freeChampionIds()
        );
    }
}
