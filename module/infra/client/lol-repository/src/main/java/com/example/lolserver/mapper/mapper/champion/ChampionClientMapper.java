package com.example.lolserver.mapper.champion;

import com.example.lolserver.restclient.summoner.model.ChampionInfo;
import com.example.lolserver.domain.champion.domain.ChampionRotate;
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