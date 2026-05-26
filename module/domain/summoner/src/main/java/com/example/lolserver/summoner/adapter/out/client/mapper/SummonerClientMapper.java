package com.example.lolserver.summoner.adapter.out.client.mapper;

import com.example.lolserver.summoner.adapter.out.client.restclient.summoner.model.SummonerVO;
import com.example.lolserver.summoner.domain.Summoner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SummonerClientMapper {

    SummonerClientMapper INSTANCE = Mappers.getMapper(SummonerClientMapper.class);

    @Mapping(target = "leagueSummoners", ignore = true)
    @Mapping(target = "lastRiotCallDate", ignore = true)
    @Mapping(target = "searchName", ignore = true)
    @Mapping(target = "platformId", ignore = true)
    Summoner toDomain(SummonerVO summonerVO);
}
