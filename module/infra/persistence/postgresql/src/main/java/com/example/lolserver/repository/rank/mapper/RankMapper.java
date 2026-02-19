package com.example.lolserver.repository.rank.mapper;

import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.repository.rank.entity.SummonerRankingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RankMapper {

    RankMapper INSTANCE = Mappers.getMapper(RankMapper.class);

    @Mapping(source = "tier", target = "tier")
    @Mapping(source = "rank", target = "rank")
    @Mapping(target = "champions", expression = "java(mapChampions(entity))")
    Rank entityToDomain(SummonerRankingEntity entity);

    default List<String> mapChampions(SummonerRankingEntity entity) {
        List<String> champions = new ArrayList<>();
        if (entity.getMostChampion1() != null && !entity.getMostChampion1().isEmpty()) {
            champions.add(entity.getMostChampion1());
        }
        if (entity.getMostChampion2() != null && !entity.getMostChampion2().isEmpty()) {
            champions.add(entity.getMostChampion2());
        }
        if (entity.getMostChampion3() != null && !entity.getMostChampion3().isEmpty()) {
            champions.add(entity.getMostChampion3());
        }
        return champions;
    }
}
