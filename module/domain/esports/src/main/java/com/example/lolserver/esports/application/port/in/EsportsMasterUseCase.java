package com.example.lolserver.esports.application.port.in;

import com.example.lolserver.esports.application.command.CorrectTeamNameCommand;
import com.example.lolserver.esports.application.command.CreateTeamCommand;
import com.example.lolserver.esports.application.command.LeagueCommand;
import com.example.lolserver.esports.application.command.RenameTeamCommand;
import com.example.lolserver.esports.application.command.SeasonCommand;
import com.example.lolserver.esports.application.command.TeamNameHistoryCommand;
import com.example.lolserver.esports.application.command.UpdateTeamCommand;
import com.example.lolserver.esports.domain.League;
import com.example.lolserver.esports.domain.Season;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamNameHistory;

import java.time.LocalDate;

/** 관리자 — 리그·시즌·팀 마스터 데이터. */
public interface EsportsMasterUseCase {

    League createLeague(LeagueCommand command);

    League updateLeague(String leagueId, LeagueCommand command);

    Season createSeason(String leagueId, SeasonCommand command);

    Season updateSeason(String seasonId, SeasonCommand command);

    Team createTeam(CreateTeamCommand command);

    Team updateTeam(String teamId, UpdateTeamCommand command);

    Team renameTeam(String teamId, RenameTeamCommand command);

    Team correctCurrentName(String teamId, CorrectTeamNameCommand command);

    TeamNameHistory updateNameHistory(String teamId, Long historyId, TeamNameHistoryCommand command);

    void deleteNameHistory(String teamId, Long historyId);

    Team disbandTeam(String teamId, LocalDate disbandedAt);
}
