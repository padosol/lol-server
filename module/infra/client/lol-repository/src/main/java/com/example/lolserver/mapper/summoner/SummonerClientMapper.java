package com.example.lolserver.mapper.summoner;

import com.example.lolserver.restclient.summoner.model.SummonerVO;
import com.example.lolserver.domain.summoner.domain.Summoner;
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
