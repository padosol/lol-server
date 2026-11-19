package com.example.lolserver.esports.adapter.out.persistence.query;

import com.example.lolserver.common.support.PageResult;
import com.example.lolserver.esports.EsportsPostgresTestBase;
import com.example.lolserver.esports.application.command.PersonSearchCommand;
import com.example.lolserver.esports.application.model.readmodel.PersonCareerReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonSummaryReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterEntryReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonRosterReadModel;
import com.example.lolserver.esports.application.model.readmodel.SummonerLinkReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamDetailReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamMemberReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamRosterReadModel;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.domain.vo.CareerEventType;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 뷰 조회 어댑터. 뷰가 CURRENT_DATE 기준이라 픽스처 날짜는 모두 오늘 기준 상대값이다. */
class EsportsQueryAdapterTest extends EsportsPostgresTestBase {

    @Autowired
    private EsportsQueryPort queryPort;

    @BeforeEach
    void setUp() {
        sql("""
                INSERT INTO esports_league (league_id, name, sort_order) VALUES ('lck', 'LCK', 10), ('lck_cl', 'LCK CL', 20);
                INSERT INTO esports_season (season_id, league_id, name, year, start_date, end_date)
                VALUES ('lck_2026', 'lck', '2026 LCK', 2026, CURRENT_DATE - 300, CURRENT_DATE - 30);
                INSERT INTO esports_team (team_id, team_code, name, home_league_id, name_since) VALUES
                    ('t1', 'T1', 'T1', 'lck', CURRENT_DATE - 199), ('gen', 'GEN', 'Gen.G', 'lck', NULL);
                INSERT INTO esports_team (team_id, team_code, name, home_league_id, parent_team_id) VALUES
                    ('t1a', 'T1A', 'T1 Esports Academy', 'lck_cl', 't1'),
                    ('gena', 'GGA', 'Gen.G Global Academy', 'lck_cl', 'gen');
                INSERT INTO esports_team (team_id, team_code, name, disbanded_at) VALUES ('old', 'OLD', 'Old', CURRENT_DATE - 900);
                INSERT INTO esports_team_name_history (team_id, team_code, name, valid_from, valid_to)
                VALUES ('t1', 'SKT', 'SK telecom T1', CURRENT_DATE - 4000, CURRENT_DATE - 200);
                INSERT INTO person (person_id, nick_name, real_name) VALUES
                    ('p_move', 'Move', NULL), ('p_up', 'Up', NULL), ('p_loan', 'Loan', NULL), ('p_ret', 'Ret', NULL),
                    ('p_ret2', 'Ret2', NULL), ('p_hiatus', 'Hiatus', '김휴식'), ('p_bad', 'Bad', NULL),
                    ('p_coach_swap', 'Swap', NULL), ('p_creator', 'Creator', NULL);

                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_move', 't1a', 'PLAYER', 'MID', 'SIGN', CURRENT_DATE - 400, CURRENT_DATE + 200, CURRENT_DATE - 101, 'MOVED'),
                    ('p_ret', 'gen', 'PLAYER', 'AD', 'SIGN', CURRENT_DATE - 500, CURRENT_DATE + 200, CURRENT_DATE - 30, 'RETIRED'),
                    ('p_ret2', 'gena', 'PLAYER', 'SPT', 'SIGN', CURRENT_DATE - 500, CURRENT_DATE + 200, CURRENT_DATE - 60, 'RETIRED'),
                    ('p_bad', 't1a', 'PLAYER', 'AD', 'SIGN', CURRENT_DATE - 200, CURRENT_DATE + 100, CURRENT_DATE - 51, 'RELEASED'),
                    ('p_coach_swap', 't1a', 'PLAYER', 'TOP', 'SIGN', CURRENT_DATE - 200, CURRENT_DATE + 100, CURRENT_DATE - 41, 'MOVED');
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_move', 't1', 'PLAYER', 'MID', 'MOVE', CURRENT_DATE - 100, CURRENT_DATE + 200),
                    ('p_up', 't1a', 'PLAYER', 'TOP', 'SIGN', CURRENT_DATE - 800, CURRENT_DATE - 366),
                    ('p_up', 't1', 'PLAYER', 'TOP', 'RENEWAL', CURRENT_DATE - 365, CURRENT_DATE + 100),
                    ('p_loan', 'gen', 'PLAYER', 'JGL', 'SIGN', CURRENT_DATE - 300, CURRENT_DATE + 300),
                    ('p_loan', 't1', 'PLAYER', 'JGL', 'LOAN', CURRENT_DATE - 10, CURRENT_DATE + 50),
                    ('p_ret2', 't1', 'PLAYER', 'SPT', 'SIGN', CURRENT_DATE - 10, CURRENT_DATE + 300),
                    ('p_hiatus', 't1', 'PLAYER', 'SPT', 'SIGN', CURRENT_DATE - 100, CURRENT_DATE + 100),
                    ('p_bad', 't1', 'PLAYER', 'AD', 'MOVE', CURRENT_DATE - 50, CURRENT_DATE + 150),
                    ('p_coach_swap', 't1', 'COACH', NULL, 'MOVE', CURRENT_DATE - 40, CURRENT_DATE + 100);
                INSERT INTO esports_career_event (person_id, event_type, event_date, team_id, reason) VALUES
                    ('p_ret', 'RETIRE', CURRENT_DATE - 30, 'gen', NULL),
                    ('p_ret2', 'RETIRE', CURRENT_DATE - 60, 'gena', NULL),
                    ('p_ret2', 'UNRETIRE', CURRENT_DATE - 20, NULL, NULL),
                    ('p_hiatus', 'HIATUS', CURRENT_DATE - 5, 't1', 'MILITARY');
                INSERT INTO person_summoner (puuid, person_id, is_main, visible) VALUES
                    ('puuid-v', 'p_hiatus', TRUE, TRUE), ('puuid-h', 'p_hiatus', FALSE, FALSE),
                    ('puuid-c', 'p_creator', TRUE, TRUE);
                """);
    }

    @DisplayName("리그로 팀을 거르고, 해체된 팀은 기본으로 빠진다")
    @Test
    void findTeams_리그로_거르고_해체팀은_뺀다() {
        assertThat(queryPort.findTeams("lck", false)).extracting(TeamReadModel::teamId)
                .containsExactlyInAnyOrder("t1", "gen");
        assertThat(queryPort.findTeams(null, false)).extracting(TeamReadModel::teamId).doesNotContain("old");
        assertThat(queryPort.findTeams(null, true)).extracting(TeamReadModel::teamId).contains("old");
    }

    @DisplayName("팀 상세는 지나간 이름과 같은 조직의 다른 팀을 함께 내려준다")
    @Test
    void findTeamDetail_지나간이름과_같은조직팀() {
        TeamDetailReadModel detail = queryPort.findTeamDetail("t1").orElseThrow();

        assertThat(detail.team().name()).isEqualTo("T1");
        assertThat(detail.pastNames()).singleElement().satisfies(n -> assertThat(n.teamCode()).isEqualTo("SKT"));
        assertThat(detail.organizationTeams()).extracting(TeamReadModel::teamId).containsExactly("t1a");
        assertThat(queryPort.findTeamDetail("t1a").orElseThrow().organizationTeams())
                .extracting(TeamReadModel::teamId).containsExactly("t1");
    }

    @DisplayName("임대 선수는 임차팀 로스터에 있고, 원 소속팀에는 loanedOut 으로 나온다")
    @Test
    void findCurrentRoster_임차팀에는_임대선수가_있고_원소속팀에는_loanedOut으로_나온다() {
        TeamRosterReadModel t1 = queryPort.findCurrentRoster("t1");
        TeamRosterReadModel gen = queryPort.findCurrentRoster("gen");

        assertThat(t1.players()).filteredOn(p -> p.personId().equals("p_loan")).singleElement()
                .satisfies(p -> {
                    assertThat(p.onLoan()).isTrue();
                    assertThat(p.loanFromTeamId()).isEqualTo("gen");
                });
        assertThat(gen.players()).extracting(RosterEntryReadModel::personId).doesNotContain("p_loan");
        assertThat(gen.loanedOut()).singleElement().satisfies(l -> {
            assertThat(l.personId()).isEqualTo("p_loan");
            assertThat(l.loanTeamId()).isEqualTo("t1");
        });
    }

    @DisplayName("선수는 포지션 순, 스태프는 따로 나온다")
    @Test
    void findCurrentRoster_선수는_포지션순() {
        TeamRosterReadModel t1 = queryPort.findCurrentRoster("t1");

        assertThat(t1.players()).extracting(RosterEntryReadModel::position)
                .containsExactly(Position.TOP, Position.JGL, Position.MID, Position.AD, Position.SPT, Position.SPT);
        assertThat(t1.players()).filteredOn(p -> p.personId().equals("p_hiatus")).singleElement()
                .satisfies(p -> assertThat(p.inactive()).isTrue());
        assertThat(t1.staff()).extracting(RosterEntryReadModel::personId).containsExactly("p_coach_swap");
        assertThat(t1.staff()).extracting(RosterEntryReadModel::role).containsExactly(Role.COACH);
    }

    @DisplayName("T1A 로 조회하면 T1A 를 거친 사람만, 조직 단위 소속 기간과 이 팀 구간이 나온다")
    @Test
    void findTeamMembers_T1A로_조회하면_T1A를_거친사람만() {
        List<TeamMemberReadModel> members = queryPort.findTeamMembers("t1a");

        assertThat(members).extracting(m -> m.tenure().personId())
                .contains("p_move", "p_up")
                .doesNotContain("p_hiatus", "p_loan");
        TeamMemberReadModel move = members.stream().filter(m -> m.tenure().personId().equals("p_move"))
                .findFirst().orElseThrow();
        assertThat(move.tenure().organizationId()).isEqualTo("t1");
        assertThat(move.tenure().teamIds()).containsExactlyInAnyOrder("t1", "t1a");
        assertThat(move.teamLastDay()).isEqualTo(daysFromToday(-101));
    }

    @DisplayName("시즌 로스터는 그 시즌 당시 팀명을 쓴다")
    @Test
    void findSeasonRoster_당시_팀명을_쓴다() {
        List<SeasonRosterReadModel> roster = queryPort.findSeasonRoster("lck_2026", "t1");

        assertThat(roster).isNotEmpty();
        assertThat(roster).extracting(SeasonRosterReadModel::teamCodeAtSeason).containsOnly("SKT");
        assertThat(roster).extracting(SeasonRosterReadModel::personId).contains("p_up");
    }

    @DisplayName("이력 없는 인물은 q 검색에만, 상태 없이 나온다 (FA 로 보이면 안 된다)")
    @Test
    void searchPersons_이력없는인물은_q검색에만_상태없이_나온다() {
        PageResult<PersonSummaryReadModel> all = queryPort.searchPersons(search(null, null, null));
        PageResult<PersonSummaryReadModel> byName = queryPort.searchPersons(search("creator", null, null));

        assertThat(all.content()).extracting(PersonSummaryReadModel::personId).doesNotContain("p_creator");
        assertThat(byName.content()).singleElement().satisfies(p -> {
            assertThat(p.personId()).isEqualTo("p_creator");
            assertThat(p.status()).isNull();
        });
    }

    @DisplayName("상태와 계약 종료일로 거른다 (스토브리그 만료 예정자)")
    @Test
    void searchPersons_상태와_계약종료일로_거른다() {
        PageResult<PersonSummaryReadModel> ending = queryPort.searchPersons(
                search(null, PersonStatus.CONTRACTED, daysFromToday(150)));

        assertThat(ending.content()).extracting(PersonSummaryReadModel::personId)
                .contains("p_hiatus")
                .doesNotContain("p_move", "p_loan", "p_ret");
        assertThat(ending.totalElements()).isEqualTo(ending.content().size());
    }

    @DisplayName("프로필의 소환사 목록에서 비공개 계정은 빠진다")
    @Test
    void findPersonProfile_비공개계정은_빠진다() {
        assertThat(queryPort.findPersonProfile("p_hiatus").orElseThrow().summoners())
                .extracting(SummonerLinkReadModel::puuid).containsExactly("puuid-v");
    }

    @DisplayName("비공개 계정으로는 인물을 찾지 않는다")
    @Test
    void findPersonByPuuid_비공개계정은_empty() {
        assertThat(queryPort.findPersonByPuuid("puuid-h")).isEmpty();
        assertThat(queryPort.findPersonByPuuid("puuid-v")).hasValueSatisfying(p -> {
            assertThat(p.personId()).isEqualTo("p_hiatus");
            assertThat(p.teamCode()).isEqualTo("T1");
            assertThat(p.status().status()).isEqualTo(PersonStatus.CONTRACTED);
        });
    }

    @DisplayName("이력 없는 인물의 소환사로 찾으면 인물은 나오고 상태는 null")
    @Test
    void findPersonByPuuid_이력없는인물은_status_null() {
        assertThat(queryPort.findPersonByPuuid("puuid-c")).hasValueSatisfying(p -> {
            assertThat(p.personId()).isEqualTo("p_creator");
            assertThat(p.status()).isNull();
        });
    }

    @DisplayName("커리어는 소속 기간·계약·사건을 시간순으로 내려준다")
    @Test
    void findPersonCareer_소속기간_계약_사건을_시간순으로() {
        PersonCareerReadModel career = queryPort.findPersonCareer("p_ret2").orElseThrow();

        assertThat(career.tenures()).extracting(t -> t.organizationId()).containsExactly("gen", "t1");
        assertThat(career.contracts()).extracting(c -> c.teamId()).containsExactly("gena", "t1");
        assertThat(career.events()).extracting(e -> e.eventType())
                .containsExactly(CareerEventType.RETIRE, CareerEventType.UNRETIRE);
        assertThat(queryPort.findPersonCareer("nobody")).isEmpty();
    }

    @DisplayName("검증 결과는 v_esports_roster_invalid 를 그대로 읽는다")
    @Test
    void findRosterIssues_검증뷰를_그대로_읽는다() {
        assertThat(queryPort.findRosterIssues())
                .anySatisfy(issue -> {
                    assertThat(issue.problem()).isEqualTo("MOVE_NOT_CONTINUOUS");
                    assertThat(issue.personId()).isEqualTo("p_bad");
                    assertThat(issue.contractId()).isNotNull();
                });
    }

    private LocalDate daysFromToday(int days) {
        return jdbcTemplate.queryForObject("SELECT CURRENT_DATE + " + days, LocalDate.class);
    }

    private static PersonSearchCommand search(String q, PersonStatus status, LocalDate contractEndingBy) {
        return new PersonSearchCommand(q, status, null, null, contractEndingBy, 0, 50);
    }
}
