package com.example.lolserver.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * V5 유도 뷰(조직 단위 재계약·소속 기간, 인물 상태, 검증 뷰)를 실제 Postgres 에서 검증한다.
 *
 * <p>뷰는 {@code CURRENT_DATE} 기준이라 픽스처 날짜도 모두 오늘 기준 상대값({@code CURRENT_DATE ± n})이다.
 * 고정 날짜를 쓰면 날짜가 지나면서 테스트가 깨진다.
 */
@Testcontainers(disabledWithoutDocker = true)
class EsportsRosterViewTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void migrateAndLoadFixtures() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/app")
                .schemas("app")
                .defaultSchema("app")
                .createSchemas(true)
                .load()
                .migrate();
        execute("""
                INSERT INTO esports_team (team_id, team_code, name) VALUES ('t1', 'T1', 'T1'), ('gen', 'GEN', 'Gen.G');
                INSERT INTO esports_team (team_id, team_code, name, parent_team_id) VALUES
                    ('t1a', 'T1A', 'T1 Esports Academy', 't1'),
                    ('gena', 'GGA', 'Gen.G Global Academy', 'gen');
                INSERT INTO esports_team (team_id, team_code, name, parent_team_id) VALUES ('x', 'X', 'Nested', 't1a');
                INSERT INTO person (person_id, nick_name) VALUES
                    ('p_move', 'Move'), ('p_up', 'Up'), ('p_loan', 'Loan'), ('p_ret', 'Ret'), ('p_ret2', 'Ret2'),
                    ('p_hiatus', 'Hiatus'), ('p_bad', 'Bad'), ('p_coach_swap', 'Swap'), ('p_creator', 'Creator'),
                    ('p_loan_ret', 'LoanRet'), ('p_loan_ret2', 'LoanRet2'), ('p_ret_future', 'RetFuture'),
                    ('p_ret_lastday', 'RetLastDay'), ('p_orphan', 'Orphan');

                -- 콜업: CL 계약을 전날 MOVED 로 끝내고 1군 MOVE 계약이 end_date 를 물려받는다
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_move', 't1a', 'PLAYER', 'MID', 'SIGN', CURRENT_DATE - 400, CURRENT_DATE + 200, CURRENT_DATE - 101, 'MOVED');
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_move', 't1', 'PLAYER', 'MID', 'MOVE', CURRENT_DATE - 100, CURRENT_DATE + 200);

                -- 재계약하며 승격: CL 계약 만료 다음 날 1군 RENEWAL
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_up', 't1a', 'PLAYER', 'TOP', 'SIGN', CURRENT_DATE - 800, CURRENT_DATE - 366),
                    ('p_up', 't1', 'PLAYER', 'TOP', 'RENEWAL', CURRENT_DATE - 365, CURRENT_DATE + 100);

                -- 임대: 원 계약(gen)은 그대로, 임대(t1)가 겹친다
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_loan', 'gen', 'PLAYER', 'JGL', 'SIGN', CURRENT_DATE - 300, CURRENT_DATE + 300),
                    ('p_loan', 't1', 'PLAYER', 'JGL', 'LOAN', CURRENT_DATE - 10, CURRENT_DATE + 50);

                -- 계약 중 은퇴: 원 소속팀(gen)이 원래 만료일까지 권리를 가진다
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_ret', 'gen', 'PLAYER', 'AD', 'SIGN', CURRENT_DATE - 500, CURRENT_DATE + 200, CURRENT_DATE - 30, 'RETIRED');
                INSERT INTO esports_career_event (person_id, event_type, event_date, team_id) VALUES
                    ('p_ret', 'RETIRE', CURRENT_DATE - 30, 'gen');

                -- 권리 기간 중 은퇴 번복 후 다른 조직(t1)과 입단
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_ret2', 'gena', 'PLAYER', 'SPT', 'SIGN', CURRENT_DATE - 500, CURRENT_DATE + 200, CURRENT_DATE - 60, 'RETIRED');
                INSERT INTO esports_career_event (person_id, event_type, event_date, team_id) VALUES
                    ('p_ret2', 'RETIRE', CURRENT_DATE - 60, 'gena'),
                    ('p_ret2', 'UNRETIRE', CURRENT_DATE - 20, NULL);
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_ret2', 't1', 'PLAYER', 'SPT', 'SIGN', CURRENT_DATE - 10, CURRENT_DATE + 300);

                -- 계약 유지 중 군 입대
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_hiatus', 't1', 'PLAYER', 'SPT', 'SIGN', CURRENT_DATE - 100, CURRENT_DATE + 100);
                INSERT INTO esports_career_event (person_id, event_type, event_date, reason) VALUES
                    ('p_hiatus', 'HIATUS', CURRENT_DATE - 5, 'MILITARY');

                -- 오입력: 직전 계약이 MOVED 가 아니고, end_date 도 바뀐 MOVE
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_bad', 't1a', 'PLAYER', 'AD', 'SIGN', CURRENT_DATE - 200, CURRENT_DATE + 100, CURRENT_DATE - 51, 'RELEASED');
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_bad', 't1', 'PLAYER', 'AD', 'MOVE', CURRENT_DATE - 50, CURRENT_DATE + 150);

                -- 오입력: 선수 계약에서 코치 계약으로 MOVE
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_coach_swap', 't1a', 'PLAYER', 'TOP', 'SIGN', CURRENT_DATE - 200, CURRENT_DATE + 100, CURRENT_DATE - 41, 'MOVED');
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_coach_swap', 't1', 'COACH', NULL, 'MOVE', CURRENT_DATE - 40, CURRENT_DATE + 100);

                -- 임대 중 은퇴: 원 계약(gen)과 임대(t1)가 함께 RETIRED. 권리는 원 소속팀(gen)이 원래 만료일까지
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_loan_ret', 'gen', 'PLAYER', 'MID', 'SIGN', CURRENT_DATE - 500, CURRENT_DATE + 400, CURRENT_DATE - 30, 'RETIRED'),
                    ('p_loan_ret', 't1', 'PLAYER', 'MID', 'LOAN', CURRENT_DATE - 100, CURRENT_DATE + 100, CURRENT_DATE - 30, 'RETIRED'),
                    ('p_loan_ret2', 'gen', 'PLAYER', 'MID', 'SIGN', CURRENT_DATE - 500, CURRENT_DATE + 400, CURRENT_DATE - 30, 'RETIRED'),
                    ('p_loan_ret2', 't1', 'PLAYER', 'MID', 'LOAN', CURRENT_DATE - 100, CURRENT_DATE + 100, CURRENT_DATE - 30, 'RETIRED');
                INSERT INTO esports_career_event (person_id, event_type, event_date, team_id) VALUES
                    ('p_loan_ret', 'RETIRE', CURRENT_DATE - 30, 'gen'),
                    ('p_loan_ret2', 'RETIRE', CURRENT_DATE - 30, 'gen'),
                    ('p_loan_ret2', 'UNRETIRE', CURRENT_DATE - 20, NULL);
                -- 권리 기간 중 원 소속팀(gen)과 다시 입단 — 권리 위반이 아니다
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date) VALUES
                    ('p_loan_ret2', 'gen', 'PLAYER', 'MID', 'SIGN', CURRENT_DATE - 10, CURRENT_DATE + 300);

                -- 발표 후 효력 전 은퇴(미래 날짜): 아직 계약 중이므로 권리를 붙이지 않는다
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_ret_future', 'gen', 'PLAYER', 'TOP', 'SIGN', CURRENT_DATE - 100, CURRENT_DATE + 200, CURRENT_DATE + 10, 'RETIRED'),
                    ('p_ret_lastday', 'gen', 'PLAYER', 'JGL', 'SIGN', CURRENT_DATE - 500, CURRENT_DATE, CURRENT_DATE - 30, 'RETIRED');
                INSERT INTO esports_career_event (person_id, event_type, event_date, team_id) VALUES
                    ('p_ret_future', 'RETIRE', CURRENT_DATE + 10, 'gen'),
                    ('p_ret_lastday', 'RETIRE', CURRENT_DATE - 30, 'gen');

                -- 정정 사고: 이적으로 끝났는데 이적 계약이 지워져 후속 계약이 없다
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type,
                                              start_date, end_date, actual_end_date, end_reason) VALUES
                    ('p_orphan', 't1', 'PLAYER', 'AD', 'SIGN', CURRENT_DATE - 200, CURRENT_DATE + 200, CURRENT_DATE - 50, 'TRANSFERRED');
                """);
    }

    @DisplayName("임대 중 은퇴하면 권리는 임차팀이 아니라 원 소속팀이 원래 만료일까지 가진다")
    @Test
    void person_status_임대중_은퇴는_원소속팀_권리() throws SQLException {
        Map<String, Object> row = personStatus("p_loan_ret");

        assertThat(row.get("status")).isEqualTo("RETIRED");
        assertThat(row.get("rights_team_id")).isEqualTo("gen");
        assertThat(row.get("rights_until")).isEqualTo(daysFromToday(400));
    }

    @DisplayName("권리 기간 중 원 소속팀과 다시 입단하면 권리 위반으로 잡지 않는다")
    @Test
    void roster_invalid_원소속팀_재입단은_권리위반이_아니다() throws SQLException {
        assertThat(rows("SELECT problem, person_id FROM v_esports_roster_invalid WHERE person_id = 'p_loan_ret2'"))
                .extracting(row -> row.get("problem"))
                .doesNotContain("SIGN_DURING_RETIRE_RIGHTS");
    }

    @DisplayName("미래 날짜로 넣은 은퇴(효력 전)는 아직 계약 중이라 권리를 붙이지 않는다")
    @Test
    void person_status_미래날짜_은퇴는_권리를_붙이지_않는다() throws SQLException {
        Map<String, Object> row = personStatus("p_ret_future");

        assertThat(row.get("status")).isEqualTo("CONTRACTED");
        assertThat(row.get("rights_team_id")).isNull();
    }

    @DisplayName("권리 마지막 날(원래 만료일 당일)에도 권리가 보인다 — 기간은 양 끝 포함")
    @Test
    void person_status_권리_마지막날도_권리가_있다() throws SQLException {
        Map<String, Object> row = personStatus("p_ret_lastday");

        assertThat(row.get("rights_team_id")).isEqualTo("gen");
        assertThat(row.get("rights_until")).isEqualTo(daysFromToday(0));
    }

    @DisplayName("이적·이동·재계약으로 조기 종료됐는데 다음 날 시작하는 계약이 없으면 검증 뷰가 잡는다")
    @Test
    void roster_invalid_후속계약없는_조기종료() throws SQLException {
        List<Map<String, Object>> rows = rows("SELECT problem, person_id FROM v_esports_roster_invalid");

        assertThat(rows)
                .extracting(row -> row.get("problem"), row -> row.get("person_id"))
                .contains(tuple("EARLY_END_WITHOUT_SUCCESSOR", "p_orphan"))
                .doesNotContain(tuple("EARLY_END_WITHOUT_SUCCESSOR", "p_move"),
                        tuple("EARLY_END_WITHOUT_SUCCESSOR", "p_coach_swap"));
    }

    @DisplayName("같은 조직 다른 팀으로 다음 날 재계약하면 직전 계약은 EXPIRED 가 아니라 RENEWED")
    @Test
    void contract_status_같은조직_다음날_재계약이면_RENEWED() throws SQLException {
        List<Map<String, Object>> rows = rows("""
                SELECT end_reason, organization_id FROM v_esports_contract_status
                 WHERE person_id = 'p_up' AND contract_type = 'SIGN'
                """);

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.get("end_reason")).isEqualTo("RENEWED");
                    assertThat(row.get("organization_id")).isEqualTo("t1");
                });
    }

    @DisplayName("팀 내 이동과 재계약 승격은 조직 단위 소속 기간 한 줄로 묶인다")
    @Test
    void tenure_팀내이동과_승격은_조직단위_한줄() throws SQLException {
        for (String personId : List.of("p_move", "p_up")) {
            List<Map<String, Object>> rows = rows(
                    "SELECT organization_id, team_ids FROM v_esports_tenure WHERE person_id = '" + personId + "'");

            assertThat(rows).as(personId).singleElement()
                    .satisfies(row -> {
                        assertThat(row.get("organization_id")).isEqualTo("t1");
                        assertThat((List<Object>) row.get("team_ids")).containsExactlyInAnyOrder("t1", "t1a");
                    });
        }
    }

    @DisplayName("임대 중이면 ON_LOAN — 지금 뛰는 팀은 임차팀, 원 계약 팀은 따로")
    @Test
    void person_status_임대중() throws SQLException {
        Map<String, Object> row = personStatus("p_loan");

        assertThat(row.get("status")).isEqualTo("ON_LOAN");
        assertThat(row.get("team_id")).isEqualTo("t1");
        assertThat(row.get("contract_team_id")).isEqualTo("gen");
    }

    @DisplayName("현재 로스터에서 임대 선수는 임차팀에만 보인다")
    @Test
    void current_roster_임대선수는_임차팀에만() throws SQLException {
        List<Map<String, Object>> rows = rows(
                "SELECT team_id FROM v_esports_current_roster WHERE person_id = 'p_loan'");

        assertThat(rows).extracting(row -> row.get("team_id")).containsExactly("t1");
    }

    @DisplayName("계약 중 은퇴하면 RETIRED 이고 원 소속팀 권리가 원래 만료일까지 남는다")
    @Test
    void person_status_계약중은퇴_권리() throws SQLException {
        Map<String, Object> row = personStatus("p_ret");

        assertThat(row.get("status")).isEqualTo("RETIRED");
        assertThat(row.get("rights_team_id")).isEqualTo("gen");
        assertThat(row.get("rights_until")).isEqualTo(daysFromToday(200));
    }

    @DisplayName("계약을 유지한 채 비활동이면 CONTRACTED + inactive + 사유")
    @Test
    void person_status_비활동() throws SQLException {
        Map<String, Object> row = personStatus("p_hiatus");

        assertThat(row.get("status")).isEqualTo("CONTRACTED");
        assertThat(row.get("inactive")).isEqualTo(true);
        assertThat(row.get("inactive_reason")).isEqualTo("MILITARY");
    }

    @DisplayName("계약·사건 이력이 없는 인물은 상태 뷰에 나오지 않는다 (FA 로 보이면 안 된다)")
    @Test
    void person_status_이력없는인물은_없다() throws SQLException {
        assertThat(rows("SELECT 1 FROM v_esports_person_status WHERE person_id = 'p_creator'")).isEmpty();
    }

    @DisplayName("검증 뷰 — 팀 내 이동 오입력, 은퇴 권리 기간 중 입단, 2단계 중첩 팀을 잡는다")
    @Test
    void roster_invalid_새검사() throws SQLException {
        List<Map<String, Object>> rows = rows("SELECT problem, person_id, team_id FROM v_esports_roster_invalid");

        assertThat(rows)
                .extracting(row -> row.get("problem"), row -> row.get("person_id"))
                .contains(
                        tuple("MOVE_NOT_CONTINUOUS", "p_bad"),
                        tuple("MOVE_END_DATE_CHANGED", "p_bad"),
                        tuple("MOVE_PLAYER_STAFF_SWITCH", "p_coach_swap"),
                        tuple("SIGN_DURING_RETIRE_RIGHTS", "p_ret2"));
        assertThat(rows)
                .filteredOn(row -> "TEAM_NESTED_TOO_DEEP".equals(row.get("problem")))
                .extracting(row -> row.get("team_id"))
                .containsExactly("x");
        assertThat(rows)
                .extracting(row -> row.get("person_id"))
                .doesNotContain("p_move", "p_up");
    }

    private static Map<String, Object> personStatus(String personId) throws SQLException {
        List<Map<String, Object>> rows = rows(
                "SELECT * FROM v_esports_person_status WHERE person_id = '" + personId + "'");
        assertThat(rows).as(personId).hasSize(1);
        return rows.get(0);
    }

    private static LocalDate daysFromToday(int days) throws SQLException {
        return (LocalDate) rows("SELECT CURRENT_DATE + " + days + " AS d").get(0).get("d");
    }

    /** 결과를 컬럼 이름 → 값으로 읽는다. DATE 는 {@link LocalDate}, 배열은 {@link List} 로 바꾼다. */
    private static List<Map<String, Object>> rows(String sql) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = connect(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value = rs.getObject(i);
                    if (value instanceof Array array) {
                        value = Arrays.asList((Object[]) array.getArray());
                    } else if (value instanceof Date date) {
                        value = date.toLocalDate();
                    }
                    row.put(meta.getColumnLabel(i), value);
                }
                result.add(row);
            }
        }
        return result;
    }

    private static void execute(String sql) throws SQLException {
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl() + "&currentSchema=app", POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
