package com.example.lolserver.repository.rank.mapper;

import com.example.lolserver.domain.rank.domain.Rank;
import com.example.lolserver.repository.rank.entity.RankEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RankMapper {

    RankMapper INSTANCE = Mappers.getMapper(RankMapper.class);

    @Mapping(target = "tier", expression = "java(entity.getTier().name() + \" \" + entity.getDivision().name())")
    @Mapping(target = "champions", expression = "java(mapChampionNames(entity.getChampionNames()))")
    Rank entityToDomain(RankEntity entity);

    default List<String> mapChampionNames(String championNames) {
        if (championNames == null || championNames.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(championNames.split(","));
    }
}
