package com.example.lolserver.esports.adapter.out.persistence.mapper;

import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsLeagueEntity;
import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsSeasonEntity;
import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsTeamEntity;
import com.example.lolserver.esports.adapter.out.persistence.entity.EsportsTeamNameHistoryEntity;
import com.example.lolserver.esports.adapter.out.persistence.entity.PersonEntity;
import com.example.lolserver.esports.adapter.out.persistence.entity.PersonSummonerEntity;
import com.example.lolserver.esports.domain.League;
import com.example.lolserver.esports.domain.Person;
import com.example.lolserver.esports.domain.PersonSummoner;
import com.example.lolserver.esports.domain.Season;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamNameHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** 리그·시즌·팀·팀명 이력·인물·소환사 연결 ↔ 엔티티. 타임스탬프는 DB/Hibernate 가 채운다. */
@Mapper(componentModel = "spring")
public interface EsportsMasterMapper {

    League toDomain(EsportsLeagueEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EsportsLeagueEntity toEntity(League league);

    Season toDomain(EsportsSeasonEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EsportsSeasonEntity toEntity(Season season);

    Team toDomain(EsportsTeamEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EsportsTeamEntity toEntity(Team team);

    TeamNameHistory toDomain(EsportsTeamNameHistoryEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    EsportsTeamNameHistoryEntity toEntity(TeamNameHistory history);

    Person toDomain(PersonEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PersonEntity toEntity(Person person);

    PersonSummoner toDomain(PersonSummonerEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PersonSummonerEntity toEntity(PersonSummoner summoner);
}
