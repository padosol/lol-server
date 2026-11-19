package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
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
import com.example.lolserver.esports.application.port.in.EsportsQueryUseCase;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.application.port.out.LeaguePersistencePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EsportsQueryService implements EsportsQueryUseCase {

    private final EsportsQueryPort esportsQueryPort;
    private final LeaguePersistencePort leaguePersistencePort;
    private final TeamPersistencePort teamPersistencePort;

    @Override
    public List<LeagueReadModel> getLeagues() {
        return esportsQueryPort.findLeagues();
    }

    @Override
    public List<SeasonReadModel> getSeasons(String leagueId) {
        if (!leaguePersistencePort.existsLeague(leagueId)) {
            throw new CoreException(ErrorType.ESPORTS_LEAGUE_NOT_FOUND);
        }
        return esportsQueryPort.findSeasons(leagueId);
    }

    @Override
    public List<TeamReadModel> getTeams(String leagueId, boolean includeDisbanded) {
        return esportsQueryPort.findTeams(leagueId, includeDisbanded);
    }

    @Override
    public TeamDetailReadModel getTeamDetail(String teamId) {
        return esportsQueryPort.findTeamDetail(teamId)
                .orElseThrow(() -> new CoreException(ErrorType.ESPORTS_TEAM_NOT_FOUND));
    }

    @Override
    public TeamRosterReadModel getCurrentRoster(String teamId) {
        validateTeam(teamId);
        return esportsQueryPort.findCurrentRoster(teamId);
    }

    @Override
    public List<TeamMemberReadModel> getTeamMembers(String teamId) {
        validateTeam(teamId);
        return esportsQueryPort.findTeamMembers(teamId);
    }

    @Override
    public List<SeasonRosterReadModel> getSeasonRoster(String seasonId, String teamId) {
        if (!leaguePersistencePort.existsSeason(seasonId)) {
            throw new CoreException(ErrorType.ESPORTS_SEASON_NOT_FOUND);
        }
        return esportsQueryPort.findSeasonRoster(seasonId, teamId);
    }

    @Override
    public PageResult<PersonSummaryReadModel> searchPersons(PersonSearchCommand command) {
        return esportsQueryPort.searchPersons(command);
    }

    @Override
    public PersonProfileReadModel getPersonProfile(String personId) {
        return esportsQueryPort.findPersonProfile(personId)
                .orElseThrow(() -> new CoreException(ErrorType.PERSON_NOT_FOUND));
    }

    @Override
    public PersonCareerReadModel getPersonCareer(String personId) {
        return esportsQueryPort.findPersonCareer(personId)
                .orElseThrow(() -> new CoreException(ErrorType.PERSON_NOT_FOUND));
    }

    @Override
    public PersonSummaryReadModel getPersonByPuuid(String puuid) {
        return esportsQueryPort.findPersonByPuuid(puuid)
                .orElseThrow(() -> new CoreException(ErrorType.PERSON_NOT_FOUND));
    }

    private void validateTeam(String teamId) {
        if (!teamPersistencePort.exists(teamId)) {
            throw new CoreException(ErrorType.ESPORTS_TEAM_NOT_FOUND);
        }
    }
}
