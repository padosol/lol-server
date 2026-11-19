package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.CorrectTeamNameCommand;
import com.example.lolserver.esports.application.command.CreateTeamCommand;
import com.example.lolserver.esports.application.command.LeagueCommand;
import com.example.lolserver.esports.application.command.RenameTeamCommand;
import com.example.lolserver.esports.application.command.SeasonCommand;
import com.example.lolserver.esports.application.command.TeamNameHistoryCommand;
import com.example.lolserver.esports.application.command.UpdateTeamCommand;
import com.example.lolserver.esports.application.port.in.EsportsMasterUseCase;
import com.example.lolserver.esports.application.port.out.LeaguePersistencePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.League;
import com.example.lolserver.esports.domain.Season;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamNameHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class EsportsMasterService implements EsportsMasterUseCase {

    private static final String GAME_CODE = "lol";

    private final LeaguePersistencePort leaguePersistencePort;
    private final TeamPersistencePort teamPersistencePort;

    @Override
    public League createLeague(LeagueCommand command) {
        if (leaguePersistencePort.existsLeague(command.leagueId())) {
            throw new CoreException(ErrorType.ESPORTS_ID_DUPLICATED);
        }
        return leaguePersistencePort.saveLeague(toLeague(command.leagueId(), command));
    }

    @Override
    public League updateLeague(String leagueId, LeagueCommand command) {
        if (!leaguePersistencePort.existsLeague(leagueId)) {
            throw new CoreException(ErrorType.ESPORTS_LEAGUE_NOT_FOUND);
        }
        return leaguePersistencePort.saveLeague(toLeague(leagueId, command));
    }

    @Override
    public Season createSeason(String leagueId, SeasonCommand command) {
        if (!leaguePersistencePort.existsLeague(leagueId)) {
            throw new CoreException(ErrorType.ESPORTS_LEAGUE_NOT_FOUND);
        }
        if (leaguePersistencePort.existsSeason(command.seasonId())) {
            throw new CoreException(ErrorType.ESPORTS_ID_DUPLICATED);
        }
        return leaguePersistencePort.saveSeason(toSeason(command.seasonId(), leagueId, command));
    }

    @Override
    public Season updateSeason(String seasonId, SeasonCommand command) {
        Season season = leaguePersistencePort.findSeason(seasonId)
                .orElseThrow(() -> new CoreException(ErrorType.ESPORTS_SEASON_NOT_FOUND));
        return leaguePersistencePort.saveSeason(toSeason(seasonId, season.getLeagueId(), command));
    }

    @Override
    public Team createTeam(CreateTeamCommand command) {
        if (teamPersistencePort.exists(command.teamId())) {
            throw new CoreException(ErrorType.ESPORTS_ID_DUPLICATED);
        }
        validateHomeLeague(command.homeLeagueId());
        Team team = Team.builder()
                .teamId(command.teamId())
                .gameCode(GAME_CODE)
                .teamCode(command.name().teamCode())
                .name(command.name().name())
                .nameEng(command.name().nameEng())
                .imageUrl(command.name().imageUrl())
                .darkImageUrl(command.name().darkImageUrl())
                .homeLeagueId(command.homeLeagueId())
                .parentTeamId(command.parentTeamId())
                .foundedAt(command.foundedAt())
                .nameSince(command.nameSince())
                .build();
        validateParent(team, command.parentTeamId());
        return teamPersistencePort.save(team);
    }

    @Override
    public Team updateTeam(String teamId, UpdateTeamCommand command) {
        Team team = findTeam(teamId);
        validateHomeLeague(command.homeLeagueId());
        validateParent(team, command.parentTeamId());
        team.update(command.homeLeagueId(), command.parentTeamId(), command.foundedAt());
        return teamPersistencePort.save(team);
    }

    @Override
    public Team renameTeam(String teamId, RenameTeamCommand command) {
        Team team = findTeam(teamId);
        TeamNameHistory history = team.rename(command.newName(), command.renamedOn(),
                command.previousNameFrom(), command.note());
        teamPersistencePort.saveNameHistory(history);
        return teamPersistencePort.save(team);
    }

    @Override
    public Team correctCurrentName(String teamId, CorrectTeamNameCommand command) {
        Team team = findTeam(teamId);
        team.correctCurrentName(command.name(), command.nameSince());
        return teamPersistencePort.save(team);
    }

    @Override
    public TeamNameHistory updateNameHistory(String teamId, Long historyId, TeamNameHistoryCommand command) {
        TeamNameHistory history = findNameHistory(teamId, historyId);
        history.correct(command.name(), command.validFrom(), command.validTo(), command.note());
        return teamPersistencePort.saveNameHistory(history);
    }

    @Override
    public void deleteNameHistory(String teamId, Long historyId) {
        findNameHistory(teamId, historyId);
        teamPersistencePort.deleteNameHistory(historyId);
    }

    @Override
    public Team disbandTeam(String teamId, LocalDate disbandedAt) {
        Team team = findTeam(teamId);
        if (teamPersistencePort.hasContractEffectiveOnOrAfter(teamId, disbandedAt.plusDays(1))) {
            throw new CoreException(ErrorType.TEAM_HAS_ACTIVE_CONTRACT);
        }
        team.disband(disbandedAt);
        return teamPersistencePort.save(team);
    }

    private Team findTeam(String teamId) {
        return teamPersistencePort.findById(teamId)
                .orElseThrow(() -> new CoreException(ErrorType.ESPORTS_TEAM_NOT_FOUND));
    }

    private TeamNameHistory findNameHistory(String teamId, Long historyId) {
        findTeam(teamId);
        return teamPersistencePort.findNameHistory(teamId, historyId)
                .orElseThrow(() -> new CoreException(ErrorType.TEAM_NAME_HISTORY_NOT_FOUND));
    }

    private void validateHomeLeague(String homeLeagueId) {
        if (homeLeagueId != null && !leaguePersistencePort.existsLeague(homeLeagueId)) {
            throw new CoreException(ErrorType.ESPORTS_LEAGUE_NOT_FOUND);
        }
    }

    private void validateParent(Team team, String parentTeamId) {
        if (parentTeamId == null) {
            return;
        }
        Team parent = teamPersistencePort.findById(parentTeamId)
                .orElseThrow(() -> new CoreException(ErrorType.ESPORTS_TEAM_NOT_FOUND));
        team.validateParent(parent, teamPersistencePort.hasChildTeams(team.getTeamId()));
    }

    private static League toLeague(String leagueId, LeagueCommand command) {
        return League.builder()
                .leagueId(leagueId)
                .gameCode(GAME_CODE)
                .name(command.name())
                .nameAcronym(command.nameAcronym())
                .region(command.region())
                .international(command.international())
                .imageUrl(command.imageUrl())
                .darkImageUrl(command.darkImageUrl())
                .sortOrder(command.sortOrder())
                .active(command.active())
                .build();
    }

    private static Season toSeason(String seasonId, String leagueId, SeasonCommand command) {
        Season season = Season.builder()
                .seasonId(seasonId)
                .leagueId(leagueId)
                .name(command.name())
                .nameAcronym(command.nameAcronym())
                .year(command.year())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .sortOrder(command.sortOrder())
                .build();
        season.validatePeriod();
        return season;
    }
}
