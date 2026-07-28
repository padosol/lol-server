package com.example.lolserver.summoner.adapter.out.persistence.mapper;

import com.example.lolserver.summoner.domain.LeagueSummoner;
import com.example.lolserver.summoner.domain.Summoner;
import com.example.lolserver.summoner.adapter.out.persistence.entity.LeagueSummonerEntity;
import com.example.lolserver.summoner.adapter.out.persistence.entity.SummonerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SummonerMapper {

    SummonerMapper INSTANCE = Mappers.getMapper(SummonerMapper.class);

    @Mapping(source = "leagueSummonerEntities", target = "leagueSummoners")
    Summoner toDomain(SummonerEntity summonerEntity);

    List<Summoner> toDomainList(List<SummonerEntity> summonerEntities);

    LeagueSummoner toDomain(LeagueSummonerEntity leagueSummonerEntity);

    SummonerEntity toEntity(Summoner summoner);
}
