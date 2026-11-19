package com.example.lolserver.esports.adapter.out.persistence.query;

import com.example.lolserver.common.support.PageResult;
import com.example.lolserver.esports.application.command.PersonSearchCommand;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.readmodel.LeagueReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonCareerReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonProfileReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonStatusReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonSummaryReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterEntryReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterIssueReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonRosterReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamDetailReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamMemberReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamRosterReadModel;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** V5 유도 뷰를 그대로 읽는다 — 만료·재계약 유도, 소속 기간 묶기, 임대 표시를 자바에서 다시 계산하지 않는다. */
@Component
@RequiredArgsConstructor
public class EsportsQueryAdapter implements EsportsQueryPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<LeagueReadModel> findLeagues() {
        return jdbc.query(EsportsQuerySql.LEAGUES, EsportsRowMappers.LEAGUE);
    }

    @Override
    public List<SeasonReadModel> findSeasons(String leagueId) {
        return jdbc.query(EsportsQuerySql.SEASONS, Map.of("leagueId", leagueId), EsportsRowMappers.SEASON);
    }

    @Override
    public List<TeamReadModel> findTeams(String leagueId, boolean includeDisbanded) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("leagueId", leagueId)
                .addValue("includeDisbanded", includeDisbanded);
        return jdbc.query(EsportsQuerySql.TEAMS, params, EsportsRowMappers.TEAM);
    }

    @Override
    public Optional<TeamDetailReadModel> findTeamDetail(String teamId) {
        return findTeam(teamId).map(team -> {
            String organizationId = team.parentTeamId() != null ? team.parentTeamId() : team.teamId();
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("teamId", teamId)
                    .addValue("organizationId", organizationId);
            return new TeamDetailReadModel(team,
                    jdbc.query(EsportsQuerySql.TEAM_PAST_NAMES, params, EsportsRowMappers.TEAM_NAME),
                    jdbc.query(EsportsQuerySql.ORGANIZATION_TEAMS, params, EsportsRowMappers.TEAM));
        });
    }

    @Override
    public TeamRosterReadModel findCurrentRoster(String teamId) {
        Map<String, String> params = Map.of("teamId", teamId);
        List<RosterEntryReadModel> entries =
                jdbc.query(EsportsQuerySql.CURRENT_ROSTER, params, EsportsRowMappers.ROSTER_ENTRY);
        return new TeamRosterReadModel(teamId,
                entries.stream().filter(e -> e.role() == Role.PLAYER).toList(),
                entries.stream().filter(e -> e.role() != Role.PLAYER).toList(),
                jdbc.query(EsportsQuerySql.LOANED_OUT, params, EsportsRowMappers.LOANED_OUT));
    }

    @Override
    public List<TeamMemberReadModel> findTeamMembers(String teamId) {
        return jdbc.query(EsportsQuerySql.TEAM_MEMBERS, Map.of("teamId", teamId), (rs, n) -> new TeamMemberReadModel(
                EsportsRowMappers.tenure(rs), EsportsRowMappers.date(rs, "team_start_date"),
                EsportsRowMappers.date(rs, "team_last_day")));
    }

    @Override
    public List<SeasonRosterReadModel> findSeasonRoster(String seasonId, String teamId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("seasonId", seasonId)
                .addValue("teamId", teamId);
        return jdbc.query(EsportsQuerySql.SEASON_ROSTER, params, EsportsRowMappers.SEASON_ROSTER);
    }

    @Override
    public PageResult<PersonSummaryReadModel> searchPersons(PersonSearchCommand command) {
        int size = Math.max(command.size(), 1);
        int page = Math.max(command.page(), 0);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("q", command.q())
                .addValue("pattern", command.q() != null ? "%" + escapeLike(command.q()) + "%" : null)
                .addValue("status", command.status() != null ? command.status().name() : null)
                .addValue("role", command.role() != null ? command.role().name() : null)
                .addValue("teamId", command.teamId())
                .addValue("endingBy", command.contractEndingBy())
                .addValue("size", size)
                .addValue("offset", (long) page * size);
        List<PersonSummaryReadModel> content =
                jdbc.query(EsportsQuerySql.SEARCH_PERSONS, params, EsportsRowMappers.PERSON_SUMMARY);
        Long total = jdbc.queryForObject(EsportsQuerySql.COUNT_PERSONS, params, Long.class);
        long totalElements = total != null ? total : 0L;
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResult<>(content, page, size, totalElements, totalPages, page == 0,
                page >= totalPages - 1);
    }

    @Override
    public Optional<PersonProfileReadModel> findPersonProfile(String personId) {
        Map<String, String> params = Map.of("personId", personId);
        return jdbc.query(EsportsQuerySql.PERSON_PROFILE, params, (rs, n) -> new PersonProfileReadModel(
                        rs.getString("person_id"), rs.getString("nick_name"), rs.getString("real_name"),
                        rs.getString("real_name_eng"), EsportsRowMappers.date(rs, "birth_date"),
                        rs.getString("nationality"), rs.getString("image_url"), EsportsRowMappers.status(rs),
                        jdbc.query(EsportsQuerySql.VISIBLE_SUMMONERS, params, EsportsRowMappers.SUMMONER)))
                .stream().findFirst();
    }

    @Override
    public Optional<PersonCareerReadModel> findPersonCareer(String personId) {
        Map<String, String> params = Map.of("personId", personId);
        Integer count = jdbc.queryForObject(EsportsQuerySql.PERSON_EXISTS, params, Integer.class);
        if (count == null || count == 0) {
            return Optional.empty();
        }
        return Optional.of(new PersonCareerReadModel(personId,
                jdbc.query(EsportsQuerySql.TENURES_OF_PERSON, params, EsportsRowMappers.TENURE),
                jdbc.query(EsportsQuerySql.CONTRACTS_OF_PERSON, params, EsportsRowMappers.CONTRACT),
                jdbc.query(EsportsQuerySql.EVENTS_OF_PERSON, params, EsportsRowMappers.CAREER_EVENT)));
    }

    @Override
    public Optional<PersonSummaryReadModel> findPersonByPuuid(String puuid) {
        return jdbc.query(EsportsQuerySql.PERSON_BY_PUUID, Map.of("puuid", puuid), EsportsRowMappers.PERSON_SUMMARY)
                .stream().findFirst();
    }

    @Override
    public Optional<PersonStatusReadModel> findPersonStatus(String personId) {
        return jdbc.query(EsportsQuerySql.PERSON_STATUS, Map.of("personId", personId), EsportsRowMappers.STATUS)
                .stream().findFirst();
    }

    @Override
    public List<ContractReadModel> findContracts(String personId) {
        return jdbc.query(EsportsQuerySql.CONTRACTS_OF_PERSON, Map.of("personId", personId),
                EsportsRowMappers.CONTRACT);
    }

    @Override
    public List<ContractReadModel> findContractsByIds(Collection<Long> contractIds) {
        if (contractIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(EsportsQuerySql.CONTRACTS_BY_IDS, Map.of("contractIds", contractIds),
                EsportsRowMappers.CONTRACT);
    }

    @Override
    public List<RosterIssueReadModel> findRosterIssues() {
        return jdbc.query(EsportsQuerySql.ROSTER_ISSUES, EsportsRowMappers.ROSTER_ISSUE);
    }

    private Optional<TeamReadModel> findTeam(String teamId) {
        return jdbc.query(EsportsQuerySql.TEAM_BY_ID, Map.of("teamId", teamId), EsportsRowMappers.TEAM)
                .stream().findFirst();
    }

    /** LIKE 메타문자를 글자 그대로 찾도록 escape 한다 (ESCAPE '\'). */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
