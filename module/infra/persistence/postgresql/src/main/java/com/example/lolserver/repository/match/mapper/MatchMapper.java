package com.example.lolserver.repository.match.mapper;

import com.example.lolserver.Division;
import com.example.lolserver.Tier;
import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.Match;
import com.example.lolserver.domain.match.domain.gamedata.GameInfoData;
import com.example.lolserver.domain.match.domain.gamedata.ParticipantData;
import com.example.lolserver.domain.match.domain.gamedata.TeamInfoData;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gamedata.timeline.events.SkillEvents;
import com.example.lolserver.domain.match.domain.gamedata.value.ItemValue;
import com.example.lolserver.domain.match.domain.gamedata.value.StatValue;
import com.example.lolserver.domain.match.domain.gamedata.value.Style;
import com.example.lolserver.repository.match.dto.ItemEventDTO;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.MatchTeamDTO;
import com.example.lolserver.repository.match.dto.SkillEventDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    MatchMapper INSTANCE = Mappers.getMapper(MatchMapper.class);

    Match toDomain(MatchEntity matchEntity);

    // This mapping from Match to MatchEntity is not strictly necessary for persistence
    // as we usually convert Entity to Domain and not vice-versa in read operations,
    // and for save operations, the service might create the entity directly or use another mapper.
    // However, including it for completeness if a `toEntity` is ever needed.
    MatchEntity toEntity(Match match);


    ParticipantData toDomain(MatchSummonerEntity matchSummonerEntity);

    @Mapping(target = "championId", expression = "java(mapChampionIds(matchTeamEntity))")
    @Mapping(target = "pickTurn", expression = "java(mapPickTurns(matchTeamEntity))")
    TeamInfoData toDomain(MatchTeamEntity matchTeamEntity);

    default List<Integer> mapChampionIds(MatchTeamEntity entity) {
        List<Integer> ids = new ArrayList<>();
        if (entity.getChampion1Id() > 0) {
            ids.add(entity.getChampion1Id());
        }
        if (entity.getChampion2Id() > 0) {
            ids.add(entity.getChampion2Id());
        }
        if (entity.getChampion3Id() > 0) {
            ids.add(entity.getChampion3Id());
        }
        if (entity.getChampion4Id() > 0) {
            ids.add(entity.getChampion4Id());
        }
        if (entity.getChampion5Id() > 0) {
            ids.add(entity.getChampion5Id());
        }
        return ids;
    }

    default List<Integer> mapPickTurns(MatchTeamEntity entity) {
        List<Integer> turns = new ArrayList<>();
        if (entity.getPick1Turn() > 0) {
            turns.add(entity.getPick1Turn());
        }
        if (entity.getPick2Turn() > 0) {
            turns.add(entity.getPick2Turn());
        }
        if (entity.getPick3Turn() > 0) {
            turns.add(entity.getPick3Turn());
        }
        if (entity.getPick4Turn() > 0) {
            turns.add(entity.getPick4Turn());
        }
        if (entity.getPick5Turn() > 0) {
            turns.add(entity.getPick5Turn());
        }
        return turns;
    }

    MSChampion toDomain(MSChampionDTO msChampionDTO);

    @Mapping(target = "averageTier", source = "averageTier", qualifiedByName = "mapAverageTierToString")
    @Mapping(target = "averageRank", source = "averageTier", qualifiedByName = "mapAverageTierToRank")
    GameInfoData toGameInfoData(MatchEntity matchEntity);

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
    ItemValue toDomain(com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue itemValue);
    com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue toPersistence(ItemValue itemValue);

    StatValue toDomain(com.example.lolserver.repository.match.entity.value.matchsummoner.StatValue statValue);
    com.example.lolserver.repository.match.entity.value.matchsummoner.StatValue toPersistence(StatValue statValue);

    @Mapping(target = "primaryRuneIds", expression = "java(mapStringToIntArray(styleValue.getPrimaryRuneIds()))")
    @Mapping(target = "secondaryRuneIds", expression = "java(mapStringToIntArray(styleValue.getSecondaryRuneIds()))")
    Style toDomain(StyleValue styleValue);

    default int[] mapStringToIntArray(String value) {
        if (value == null || value.isEmpty()) {
            return new int[0];
        }
        return Arrays.stream(value.split(","))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    // Mappers for timeline events
    ItemEvents toDomain(ItemEventsEntity itemEvents);
    SkillEvents toDomain(SkillEventsEntity skillEvents);

    List<ItemEvents> toDomainItemEventsList(List<ItemEventsEntity> itemEvents);
    List<SkillEvents> toDomainSkillEventsList(List<SkillEventsEntity> skillEvents);

    // DTO → Domain 매핑
    @Mapping(target = "averageTier", source = "averageTier",
            qualifiedByName = "mapAverageTierToString")
    @Mapping(target = "averageRank", source = "averageTier",
            qualifiedByName = "mapAverageTierToRank")
    GameInfoData toGameInfoData(MatchDTO dto);

    @Mapping(target = "style", source = "styleValue")
    ParticipantData toDomain(MatchSummonerDTO dto);

    @Mapping(target = "championId",
            expression = "java(mapChampionIds(dto))")
    @Mapping(target = "pickTurn",
            expression = "java(mapPickTurns(dto))")
    TeamInfoData toDomain(MatchTeamDTO dto);

    default List<Integer> mapChampionIds(MatchTeamDTO dto) {
        List<Integer> ids = new ArrayList<>();
        if (dto.getChampion1Id() > 0) {
            ids.add(dto.getChampion1Id());
        }
        if (dto.getChampion2Id() > 0) {
            ids.add(dto.getChampion2Id());
        }
        if (dto.getChampion3Id() > 0) {
            ids.add(dto.getChampion3Id());
        }
        if (dto.getChampion4Id() > 0) {
            ids.add(dto.getChampion4Id());
        }
        if (dto.getChampion5Id() > 0) {
            ids.add(dto.getChampion5Id());
        }
        return ids;
    }

    default List<Integer> mapPickTurns(MatchTeamDTO dto) {
        List<Integer> turns = new ArrayList<>();
        if (dto.getPick1Turn() > 0) {
            turns.add(dto.getPick1Turn());
        }
        if (dto.getPick2Turn() > 0) {
            turns.add(dto.getPick2Turn());
        }
        if (dto.getPick3Turn() > 0) {
            turns.add(dto.getPick3Turn());
        }
        if (dto.getPick4Turn() > 0) {
            turns.add(dto.getPick4Turn());
        }
        if (dto.getPick5Turn() > 0) {
            turns.add(dto.getPick5Turn());
        }
        return turns;
    }

    ItemEvents toDomain(ItemEventDTO dto);
    SkillEvents toDomain(SkillEventDTO dto);

    List<ItemEvents> toDomainItemEventDTOList(List<ItemEventDTO> dtos);
    List<SkillEvents> toDomainSkillEventDTOList(List<SkillEventDTO> dtos);
}