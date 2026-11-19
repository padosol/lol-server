package com.example.lolserver.esports.adapter.out.persistence;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.EsportsPostgresTestBase;
import com.example.lolserver.esports.application.port.out.PersonTimelinePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.CareerEvent;
import com.example.lolserver.esports.domain.Contract;
import com.example.lolserver.esports.domain.PersonTimeline;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.HiatusReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonTimelinePersistenceAdapterTest extends EsportsPostgresTestBase {

    @Autowired
    private PersonTimelinePort personTimelinePort;

    @Autowired
    private TeamPersistencePort teamPersistencePort;

    @BeforeEach
    void setUp() {
        sql("""
                INSERT INTO esports_team (team_id, team_code, name) VALUES ('t1', 'T1', 'T1'), ('gen', 'GEN', 'Gen.G');
                INSERT INTO esports_team (team_id, team_code, name, parent_team_id) VALUES ('t1a', 'T1A', 'T1 Academy', 't1');
                INSERT INTO person (person_id, nick_name) VALUES ('faker', 'Faker');
                """);
    }

    @DisplayName("계약을 읽을 때 조직 ID 를 상위 팀으로 채운다")
    @Test
    void load_계약의_organizationId를_상위팀으로_채운다() {
        sql("""
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date)
                VALUES ('faker', 't1a', 'PLAYER', 'MID', 'SIGN', '2026-01-01', '2026-11-16');
                """);

        PersonTimeline timeline = personTimelinePort.load("faker");

        assertThat(timeline.getContracts()).singleElement()
                .satisfies(c -> {
                    assertThat(c.getTeamId()).isEqualTo("t1a");
                    assertThat(c.getOrganizationId()).isEqualTo("t1");
                    assertThat(c.getContractType()).isEqualTo(ContractType.SIGN);
                });
    }

    @DisplayName("이적 저장은 직전 계약 조기 종료(UPDATE)를 먼저 반영해 기간 겹침 제약에 걸리지 않는다")
    @Test
    void transfer_저장은_직전계약_UPDATE를_먼저_반영한다() {
        sql("""
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date)
                VALUES ('faker', 't1', 'PLAYER', 'MID', 'SIGN', '2025-11-18', '2027-11-15');
                """);
        PersonTimeline timeline = personTimelinePort.load("faker");
        Team gen = teamPersistencePort.findById("gen").orElseThrow();
        timeline.transfer(gen, Role.PLAYER, Position.MID, LocalDate.of(2026, 11, 20), LocalDate.of(2028, 11, 20), null);

        personTimelinePort.save(timeline);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT team_id, actual_end_date, end_reason FROM esports_contract WHERE person_id = 'faker' "
                        + "ORDER BY start_date");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("actual_end_date")).hasToString("2026-11-19");
        assertThat(rows.get(0).get("end_reason")).isEqualTo("TRANSFERRED");
        assertThat(rows.get(1).get("team_id")).isEqualTo("gen");
    }

    @DisplayName("저장하면 새 계약과 사건에 생성된 ID 가 채워진다")
    @Test
    void save_생성된_계약과_사건에_ID가_채워진다() {
        PersonTimeline timeline = personTimelinePort.load("faker");
        Team t1 = teamPersistencePort.findById("t1").orElseThrow();
        Contract signed = timeline.sign(t1, Role.PLAYER, Position.MID, LocalDate.of(2026, 11, 17),
                LocalDate.of(2027, 11, 15), null);
        CareerEvent hiatus = timeline.hiatus(LocalDate.of(2026, 12, 1), HiatusReason.REST, null, null);

        personTimelinePort.save(timeline);

        assertThat(signed.getContractId()).isNotNull();
        assertThat(hiatus.getEventId()).isNotNull();
        assertThat(personTimelinePort.findContract(signed.getContractId())).isPresent();
        assertThat(personTimelinePort.findEvent(hiatus.getEventId()))
                .hasValueSatisfying(e -> assertThat(e.getReason()).isEqualTo(HiatusReason.REST));
    }

    @DisplayName("도메인 검사를 거치지 않은 겹치는 계약은 DB 제약 위반을 CONTRACT_PERIOD_CONFLICT 로 바꿔 던진다")
    @Test
    void saveContract_겹치면_CONTRACT_PERIOD_CONFLICT() {
        sql("""
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date)
                VALUES ('faker', 't1', 'PLAYER', 'MID', 'SIGN', '2025-11-18', '2027-11-15');
                """);
        Team gen = teamPersistencePort.findById("gen").orElseThrow();
        Contract overlapping = Contract.open("faker", gen, Role.PLAYER, Position.MID, ContractType.SIGN,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 11, 16), null);

        assertThatThrownBy(() -> personTimelinePort.saveContract(overlapping))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONTRACT_PERIOD_CONFLICT);
    }
}
