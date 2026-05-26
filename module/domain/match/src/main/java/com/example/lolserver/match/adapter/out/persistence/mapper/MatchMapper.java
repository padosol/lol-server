package com.example.lolserver.match.adapter.out.persistence.mapper;

import com.example.lolserver.Division;
import com.example.lolserver.Tier;
import com.example.lolserver.match.application.model.GameInfoReadModel;
import com.example.lolserver.match.application.model.ItemValueReadModel;
import com.example.lolserver.match.application.model.MSChampionDetailReadModel;
import com.example.lolserver.match.application.model.ParticipantReadModel;
import com.example.lolserver.match.application.model.StatValueReadModel;
import com.example.lolserver.match.application.model.StyleReadModel;
import com.example.lolserver.match.adapter.out.persistence.dto.MSChampionDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.MatchDTO;
import com.example.lolserver.match.adapter.out.persistence.dto.MatchSummonerDTO;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchEntity;
import com.example.lolserver.match.adapter.out.persistence.entity.MatchSummonerEntity;
import com.example.lolserver.match.adapter.out.persistence.entity.value.matchsummoner.PerkStatValue;
import com.example.lolserver.match.adapter.out.persistence.entity.value.matchsummoner.PerkStyleValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    MatchMapper INSTANCE = Mappers.getMapper(MatchMapper.class);

    @Mapping(target = "style", source = "perkStyle")
    @Mapping(target = "statValue", source = "perkStat")
    ParticipantReadModel toReadModel(MatchSummonerEntity matchSummonerEntity);

    MSChampionDetailReadModel toReadModel(MSChampionDTO msChampionDTO);

    @Mapping(target = "averageTier", source = "averageTier",
            qualifiedByName = "mapAverageTierToString")
    @Mapping(target = "averageRank", source = "averageTier",
            qualifiedByName = "mapAverageTierToRank")
    GameInfoReadModel toGameInfoReadModel(MatchEntity matchEntity);

    @Named("mapAverageTierToString")
    default String mapAverageTierToString(Integer absolutePoints) {
        if (absolutePoints == null) {
            return null;
        }
        return Tier.fromAbsolutePoints(absolutePoints).name();
    }

    @Named("mapAverageTierToRank")
    default String mapAverageTierToRank(Integer absolutePoints) {
        if (absolutePoints == null) {
            return null;
        }
        Tier tier = Tier.fromAbsolutePoints(absolutePoints);
        if (!tier.hasDivision()) {
            return null;
        }
        int remainder = absolutePoints - tier.getScore();
        return Division.fromRemainingPoints(remainder).name();
    }

    // Mappers for value objects
    ItemValueReadModel toReadModel(
            com.example.lolserver.match.adapter.out.persistence.entity.value.matchsummoner.ItemValue itemValue);

    @Mapping(target = "defense", source = "statPerkDefense")
    @Mapping(target = "flex", source = "statPerkFlex")
    @Mapping(target = "offense", source = "statPerkOffense")
    StatValueReadModel toReadModel(PerkStatValue perkStatValue);

    StyleReadModel toReadModel(PerkStyleValue perkStyleValue);

    // DTO → ReadModel 매핑
    @Mapping(target = "averageTier", source = "averageTier",
            qualifiedByName = "mapAverageTierToString")
    @Mapping(target = "averageRank", source = "averageTier",
            qualifiedByName = "mapAverageTierToRank")
    GameInfoReadModel toGameInfoReadModel(MatchDTO dto);

    @Mapping(target = "style", source = "perkStyle")
    @Mapping(target = "statValue", source = "perkStat")
    ParticipantReadModel toReadModel(MatchSummonerDTO dto);
}
