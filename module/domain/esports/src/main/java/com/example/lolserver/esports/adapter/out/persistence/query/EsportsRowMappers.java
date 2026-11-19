package com.example.lolserver.esports.adapter.out.persistence.query;

import com.example.lolserver.esports.application.model.readmodel.CareerEventReadModel;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.readmodel.LeagueReadModel;
import com.example.lolserver.esports.application.model.readmodel.LoanedOutReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonStatusReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonSummaryReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterEntryReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterIssueReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonRosterReadModel;
import com.example.lolserver.esports.application.model.readmodel.SummonerLinkReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamNameReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamReadModel;
import com.example.lolserver.esports.application.model.readmodel.TenureReadModel;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.ContractStatus;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** 뷰 행 → ReadModel. enum 은 이름 문자열, DATE 는 {@link LocalDate}, 배열은 {@link List}. */
final class EsportsRowMappers {

    private EsportsRowMappers() {
    }

    static final RowMapper<LeagueReadModel> LEAGUE = (rs, n) -> new LeagueReadModel(
            rs.getString("league_id"), rs.getString("name"), rs.getString("name_acronym"), rs.getString("region"),
            rs.getBoolean("international"), rs.getString("image_url"), rs.getString("dark_image_url"),
            rs.getInt("sort_order"), rs.getBoolean("active"));

    static final RowMapper<SeasonReadModel> SEASON = (rs, n) -> new SeasonReadModel(
            rs.getString("season_id"), rs.getString("league_id"), rs.getString("name"), rs.getString("name_acronym"),
            (Integer) rs.getObject("year"), date(rs, "start_date"), date(rs, "end_date"), rs.getInt("sort_order"));

    static final RowMapper<TeamReadModel> TEAM = (rs, n) -> new TeamReadModel(
            rs.getString("team_id"), rs.getString("team_code"), rs.getString("name"), rs.getString("name_eng"),
            rs.getString("image_url"), rs.getString("dark_image_url"), rs.getString("home_league_id"),
            rs.getString("parent_team_id"), date(rs, "founded_at"), date(rs, "disbanded_at"),
            date(rs, "name_since"));

    static final RowMapper<TeamNameReadModel> TEAM_NAME = (rs, n) -> new TeamNameReadModel(
            rs.getLong("history_id"), rs.getString("team_code"), rs.getString("name"), rs.getString("name_eng"),
            rs.getString("image_url"), rs.getString("dark_image_url"), date(rs, "valid_from"),
            date(rs, "valid_to"));

    static final RowMapper<RosterEntryReadModel> ROSTER_ENTRY = (rs, n) -> new RosterEntryReadModel(
            rs.getString("person_id"), rs.getString("nick_name"), rs.getString("image_url"),
            rs.getString("nationality"), enumOf(Role.class, rs.getString("role")),
            enumOf(Position.class, rs.getString("position")),
            enumOf(ContractType.class, rs.getString("contract_type")), date(rs, "start_date"),
            date(rs, "last_day"), rs.getBoolean("on_loan"), rs.getString("loan_from_team_id"),
            rs.getBoolean("inactive"), enumOf(HiatusReason.class, rs.getString("inactive_reason")));

    static final RowMapper<LoanedOutReadModel> LOANED_OUT = (rs, n) -> new LoanedOutReadModel(
            rs.getString("person_id"), rs.getString("nick_name"), enumOf(Position.class, rs.getString("position")),
            rs.getString("loan_team_id"), date(rs, "loan_last_day"));

    static final RowMapper<TenureReadModel> TENURE = (rs, n) -> tenure(rs);

    static final RowMapper<SeasonRosterReadModel> SEASON_ROSTER = (rs, n) -> new SeasonRosterReadModel(
            rs.getString("season_id"), rs.getString("team_id"), rs.getString("team_code_at_season"),
            rs.getString("team_name_at_season"), rs.getString("person_id"), rs.getString("nick_name"),
            enumOf(Role.class, rs.getString("role")), enumOf(Position.class, rs.getString("position")),
            enumOf(ContractType.class, rs.getString("contract_type")), date(rs, "start_date"),
            date(rs, "last_day"));

    static final RowMapper<PersonStatusReadModel> STATUS = (rs, n) -> status(rs);

    static final RowMapper<PersonSummaryReadModel> PERSON_SUMMARY = (rs, n) -> new PersonSummaryReadModel(
            rs.getString("person_id"), rs.getString("nick_name"), rs.getString("real_name"),
            rs.getString("nationality"), rs.getString("image_url"), rs.getString("team_code"), status(rs));

    static final RowMapper<SummonerLinkReadModel> SUMMONER = (rs, n) -> new SummonerLinkReadModel(
            rs.getString("puuid"), rs.getBoolean("is_main"), rs.getBoolean("visible"), rs.getString("note"));

    static final RowMapper<ContractReadModel> CONTRACT = (rs, n) -> new ContractReadModel(
            rs.getLong("contract_id"), rs.getString("team_id"), rs.getString("team_code"),
            enumOf(Role.class, rs.getString("role")), enumOf(Position.class, rs.getString("position")),
            enumOf(ContractType.class, rs.getString("contract_type")), date(rs, "start_date"),
            date(rs, "end_date"), date(rs, "actual_end_date"), date(rs, "last_day"),
            enumOf(ContractStatus.class, rs.getString("status")),
            enumOf(EndReason.class, rs.getString("end_reason")), rs.getString("note"));

    static final RowMapper<CareerEventReadModel> CAREER_EVENT = (rs, n) -> new CareerEventReadModel(
            rs.getLong("event_id"), enumOf(CareerEventType.class, rs.getString("event_type")),
            date(rs, "event_date"), rs.getString("team_id"), enumOf(HiatusReason.class, rs.getString("reason")),
            rs.getString("note"), rs.getString("source_url"));

    static final RowMapper<RosterIssueReadModel> ROSTER_ISSUE = (rs, n) -> new RosterIssueReadModel(
            rs.getString("problem"), rs.getString("person_id"), (Long) rs.getObject("contract_id"),
            rs.getString("team_id"), date(rs, "at_date"));

    static TenureReadModel tenure(ResultSet rs) throws SQLException {
        return new TenureReadModel(
                rs.getString("person_id"), rs.getString("nick_name"), rs.getString("organization_id"),
                rs.getBoolean("as_player"), date(rs, "start_date"), date(rs, "last_day"),
                enumOf(EndReason.class, rs.getString("end_reason")), rs.getInt("contract_count"),
                rs.getInt("renewal_count"), rs.getBoolean("loan"),
                strings(rs, "roles").stream().map(Role::valueOf).toList(), strings(rs, "team_ids"));
    }

    /** {@code STATUS_COLUMNS} 로 읽은 상태. 상태 뷰에 없는 인물(이력 없음)이면 null. */
    static PersonStatusReadModel status(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        if (status == null) {
            return null;
        }
        return new PersonStatusReadModel(
                PersonStatus.valueOf(status), rs.getString("st_team_id"), rs.getString("contract_team_id"),
                enumOf(Role.class, rs.getString("st_role")), enumOf(Position.class, rs.getString("st_position")),
                date(rs, "contract_last_day"), rs.getBoolean("inactive"),
                enumOf(HiatusReason.class, rs.getString("inactive_reason")), rs.getBoolean("retired_as_player"),
                rs.getString("next_team_id"), date(rs, "next_start_date"), rs.getString("rights_team_id"),
                date(rs, "rights_until"));
    }

    static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value != null ? value.toLocalDate() : null;
    }

    static <E extends Enum<E>> E enumOf(Class<E> type, String value) {
        return value != null ? Enum.valueOf(type, value) : null;
    }

    private static List<String> strings(ResultSet rs, String column) throws SQLException {
        Array array = rs.getArray(column);
        if (array == null) {
            return List.of();
        }
        return Arrays.stream((Object[]) array.getArray()).map(String::valueOf).toList();
    }
}
