package com.example.lolserver.esports.application.port.in;

import com.example.lolserver.common.support.PageResult;
import com.example.lolserver.esports.application.command.PersonSearchCommand;
import com.example.lolserver.esports.application.model.readmodel.LeagueReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonCareerReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonProfileReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonSummaryReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonRosterReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamDetailReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamMemberReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamRosterReadModel;

import java.util.List;

/** 공개 — 리그·팀·로스터·인물 조회. 단건 대상이 없으면 404. */
public interface EsportsQueryUseCase {

    List<LeagueReadModel> getLeagues();

    List<SeasonReadModel> getSeasons(String leagueId);

    List<TeamReadModel> getTeams(String leagueId, boolean includeDisbanded);

    TeamDetailReadModel getTeamDetail(String teamId);

    TeamRosterReadModel getCurrentRoster(String teamId);

    List<TeamMemberReadModel> getTeamMembers(String teamId);

    List<SeasonRosterReadModel> getSeasonRoster(String seasonId, String teamId);

    PageResult<PersonSummaryReadModel> searchPersons(PersonSearchCommand command);

    PersonProfileReadModel getPersonProfile(String personId);

    PersonCareerReadModel getPersonCareer(String personId);

    /** 공개 계정으로 연결된 인물. 연결이 없거나 비공개면 404. */
    PersonSummaryReadModel getPersonByPuuid(String puuid);
}
