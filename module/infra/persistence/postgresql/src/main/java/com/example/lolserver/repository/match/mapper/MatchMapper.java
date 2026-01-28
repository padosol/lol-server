package com.example.lolserver.repository.match.mapper;

import com.example.lolserver.domain.match.domain.MSChampion;
import com.example.lolserver.domain.match.domain.Match;
import com.example.lolserver.domain.match.domain.gameData.GameInfoData;
import com.example.lolserver.domain.match.domain.gameData.ParticipantData;
import com.example.lolserver.domain.match.domain.gameData.TeamInfoData;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.ItemEvents;
import com.example.lolserver.domain.match.domain.gameData.timeline.events.SkillEvents;
import com.example.lolserver.domain.match.domain.gameData.value.ItemValue;
import com.example.lolserver.domain.match.domain.gameData.value.StatValue;
import com.example.lolserver.domain.match.domain.gameData.value.Style;
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.StyleValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

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

    TeamInfoData toDomain(MatchTeamEntity matchTeamEntity);

    MSChampion toDomain(MSChampionDTO msChampionDTO);

    GameInfoData toGameInfoData(MatchEntity matchEntity);

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
    ItemEvents toDomain(ItemEventsEntity itemEvents); // Persistence ItemEvents to Domain ItemEvents
    SkillEvents toDomain(SkillEventsEntity skillEvents); // Persistence SkillEvents to Domain SkillEvents

    List<ItemEvents> toDomainItemEventsList(List<ItemEventsEntity> itemEvents);
    List<SkillEvents> toDomainSkillEventsList(List<SkillEventsEntity> skillEvents);
}