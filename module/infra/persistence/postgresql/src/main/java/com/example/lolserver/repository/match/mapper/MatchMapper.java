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
import com.example.lolserver.repository.match.dto.MSChampionDTO;
import com.example.lolserver.repository.match.dto.MatchDTO;
import com.example.lolserver.repository.match.dto.MatchSummonerDTO;
import com.example.lolserver.repository.match.dto.TimelineEventDTO;
import com.example.lolserver.repository.match.entity.MatchEntity;
import com.example.lolserver.repository.match.entity.MatchSummonerEntity;
import com.example.lolserver.repository.match.entity.MatchTeamEntity;
import com.example.lolserver.repository.match.entity.timeline.events.ItemEventsEntity;
import com.example.lolserver.repository.match.entity.timeline.events.SkillEventsEntity;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStatValue;
import com.example.lolserver.repository.match.entity.value.matchsummoner.PerkStyleValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    MatchMapper INSTANCE = Mappers.getMapper(MatchMapper.class);

    Match toDomain(MatchEntity matchEntity);

    MatchEntity toEntity(Match match);

    @Mapping(target = "style", source = "perkStyle")
    @Mapping(target = "statValue", source = "perkStat")
    ParticipantData toDomain(MatchSummonerEntity matchSummonerEntity);

    TeamInfoData toDomain(MatchTeamEntity matchTeamEntity);

    MSChampion toDomain(MSChampionDTO msChampionDTO);

    @Mapping(target = "averageTier", source = "averageTier",
            qualifiedByName = "mapAverageTierToString")
    @Mapping(target = "averageRank", source = "averageTier",
            qualifiedByName = "mapAverageTierToRank")
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
    ItemValue toDomain(
            com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue itemValue);
    com.example.lolserver.repository.match.entity.value.matchsummoner.ItemValue toPersistence(
            ItemValue itemValue);

    @Mapping(target = "defense", source = "statPerkDefense")
    @Mapping(target = "flex", source = "statPerkFlex")
    @Mapping(target = "offense", source = "statPerkOffense")
    StatValue toDomain(PerkStatValue perkStatValue);

    Style toDomain(PerkStyleValue perkStyleValue);

    // Mappers for timeline events
    ItemEvents toDomain(ItemEventsEntity itemEvents);
    SkillEvents toDomain(SkillEventsEntity skillEvents);

    List<ItemEvents> toDomainItemEventsList(List<ItemEventsEntity> itemEvents);
    List<SkillEvents> toDomainSkillEventsList(
            List<SkillEventsEntity> skillEvents);

    // DTO → Domain 매핑
    @Mapping(target = "averageTier", source = "averageTier",
            qualifiedByName = "mapAverageTierToString")
    @Mapping(target = "averageRank", source = "averageTier",
            qualifiedByName = "mapAverageTierToRank")
    GameInfoData toGameInfoData(MatchDTO dto);

    @Mapping(target = "style", source = "perkStyle")
    @Mapping(target = "statValue", source = "perkStat")
    ParticipantData toDomain(MatchSummonerDTO dto);

    default ItemEvents toItemEventsFromTimelineDTO(TimelineEventDTO dto) {
        return ItemEvents.builder()
                .itemId(dto.getEventId())
                .participantId(dto.getParticipantId())
                .timestamp(dto.getTimestamp())
                .type(dto.getEventType())
                .build();
    }

    default SkillEvents toSkillEventsFromTimelineDTO(TimelineEventDTO dto) {
        return SkillEvents.builder()
                .skillSlot(dto.getEventId())
                .participantId(dto.getParticipantId())
                .timestamp(dto.getTimestamp())
                .levelUpType(dto.getEventType())
                .build();
    }
}
