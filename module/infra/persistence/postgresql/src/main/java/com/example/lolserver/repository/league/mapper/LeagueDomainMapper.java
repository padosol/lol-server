package com.example.lolserver.repository.league.mapper;

import com.example.lolserver.domain.league.domain.League;
import com.example.lolserver.domain.league.domain.vo.LeagueHistory;
import com.example.lolserver.repository.league.entity.LeagueSummonerEntity;
import com.example.lolserver.repository.league.entity.LeagueSummonerHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LeagueDomainMapper {

    LeagueDomainMapper INSTANCE = Mappers.getMapper(LeagueDomainMapper.class);

    @Mapping(target = "leagueHistory", ignore = true) // History will be added separately
    @Mapping(target = "winRate", expression = "java(entity.getWins() + entity.getLosses() > 0 ? new java.math.BigDecimal(entity.getWins()).divide(new java.math.BigDecimal(entity.getWins()).add(new java.math.BigDecimal(entity.getLosses())), 2, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO)")
    League toDomain(LeagueSummonerEntity entity);

    List<LeagueHistory> toDomainHistoryList(List<LeagueSummonerHistoryEntity> entities);

    LeagueHistory toDomain(LeagueSummonerHistoryEntity entity);
}