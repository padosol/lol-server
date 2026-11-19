package com.example.lolserver.esports.adapter;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.EsportsPostgresTestBase;
import com.example.lolserver.esports.application.ContractCorrectionService;
import com.example.lolserver.esports.application.ContractEventService;
import com.example.lolserver.esports.application.command.ContractCommand;
import com.example.lolserver.esports.application.command.CorrectContractCommand;
import com.example.lolserver.esports.application.command.MoveCommand;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.model.resultmodel.ContractEventResultModel;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.application.port.out.PersonTimelinePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 사건 서비스 + 실제 어댑터 + 실제 DB 제약. 서비스는 JPA 슬라이스에 빈이 아니라 직접 만든다. */
class ContractEventFlowTest extends EsportsPostgresTestBase {

    @Autowired
    private PersonPersistencePort personPersistencePort;

    @Autowired
    private TeamPersistencePort teamPersistencePort;

    @Autowired
    private PersonTimelinePort personTimelinePort;

    @Autowired
    private EsportsQueryPort esportsQueryPort;

    private ContractEventService events;
    private ContractCorrectionService corrections;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        events = new ContractEventService(personPersistencePort, teamPersistencePort, personTimelinePort,
                esportsQueryPort);
        corrections = new ContractCorrectionService(personPersistencePort, personTimelinePort, esportsQueryPort);
        today = jdbcTemplate.queryForObject("SELECT CURRENT_DATE", LocalDate.class);
        sql("""
                INSERT INTO esports_team (team_id, team_code, name) VALUES ('t1', 'T1', 'T1'), ('gen', 'GEN', 'Gen.G');
                INSERT INTO esports_team (team_id, team_code, name, parent_team_id) VALUES ('gena', 'GGA', 'Gen.G Academy', 'gen');
                INSERT INTO person (person_id, nick_name) VALUES ('faker', 'Faker');
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date)
                VALUES ('faker', 't1', 'PLAYER', 'MID', 'SIGN', CURRENT_DATE - 365, CURRENT_DATE - 3);
                """);
    }

    @DisplayName("스토브리그 — 만료 다음 날 재계약, 이적, 팀 내 이동이 DB 제약을 통과하고 검증 뷰에 걸리지 않는다")
    @Test
    void 스토브리그_재계약_이적_이동이_DB제약을_통과한다() {
        ContractEventResultModel renewed = events.renew("faker", new ContractCommand("t1", Role.PLAYER, Position.MID,
                today.minusDays(2), today.plusDays(360), null));
        ContractEventResultModel transferred = events.transfer("faker", new ContractCommand("gen", Role.PLAYER,
                Position.MID, today.plusDays(30), today.plusDays(700), null));
        ContractEventResultModel moved = events.move("faker", new MoveCommand("gena", Role.PLAYER, Position.MID,
                today.plusDays(60), null));

        assertThat(renewed.contracts()).extracting(ContractReadModel::contractType)
                .containsExactly(ContractType.RENEWAL);
        assertThat(renewed.status().status()).isEqualTo(PersonStatus.CONTRACTED);
        assertThat(transferred.contracts()).extracting(ContractReadModel::endReason)
                .containsExactly(EndReason.TRANSFERRED, null);
        assertThat(transferred.status().nextTeamId()).isEqualTo("gen");
        assertThat(moved.contracts()).filteredOn(c -> c.contractType() == ContractType.MOVE).singleElement()
                .satisfies(c -> assertThat(c.endDate()).isEqualTo(today.plusDays(700)));
        assertThat(esportsQueryPort.findRosterIssues()).noneMatch(issue -> "faker".equals(issue.personId()));
    }

    @DisplayName("정정으로 다른 계약과 기간이 겹치게 만들면 DB 제약이 막고 409 오류로 바뀐다")
    @Test
    void correctContract_겹치는기간이면_CONTRACT_PERIOD_CONFLICT() {
        events.sign("faker", new ContractCommand("gen", Role.PLAYER, Position.MID, today.plusDays(10),
                today.plusDays(300), null));
        Long first = jdbcTemplate.queryForObject(
                "SELECT contract_id FROM esports_contract WHERE team_id = 't1'", Long.class);

        assertThatThrownBy(() -> corrections.correctContract(first, new CorrectContractCommand(
                today.minusDays(365), today.plusDays(20), null, null, Position.MID, null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONTRACT_PERIOD_CONFLICT);
        // 제약 위반 뒤에는 Postgres 트랜잭션이 중단되므로 같은 트랜잭션에서 더 조회하지 않는다 (서비스는 롤백된다)
    }
}
