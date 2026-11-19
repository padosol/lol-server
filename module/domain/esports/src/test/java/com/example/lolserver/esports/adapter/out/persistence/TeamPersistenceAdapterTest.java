package com.example.lolserver.esports.adapter.out.persistence;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.EsportsPostgresTestBase;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamName;
import com.example.lolserver.esports.domain.TeamNameHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamPersistenceAdapterTest extends EsportsPostgresTestBase {

    @Autowired
    private TeamPersistencePort teamPersistencePort;

    @DisplayName("저장한 팀을 다시 읽으면 상위 팀과 이름이 같다")
    @Test
    void save_후_findById_상위팀과_이름이_같다() {
        teamPersistencePort.save(Team.builder().teamId("t1").teamCode("T1").gameCode("lol").name("T1").build());
        teamPersistencePort.save(Team.builder().teamId("t1a").teamCode("T1A").gameCode("lol")
                .name("T1 Esports Academy").parentTeamId("t1").build());

        Team found = teamPersistencePort.findById("t1a").orElseThrow();

        assertThat(found.getParentTeamId()).isEqualTo("t1");
        assertThat(found.getName()).isEqualTo("T1 Esports Academy");
        assertThat(found.organizationId()).isEqualTo("t1");
    }

    @DisplayName("팀명 이력 기간이 겹치면 CONTRACT_PERIOD_CONFLICT")
    @Test
    void saveNameHistory_기간이_겹치면_CONTRACT_PERIOD_CONFLICT() {
        Team team = teamPersistencePort.save(Team.builder().teamId("t1").teamCode("T1").gameCode("lol")
                .name("T1").nameSince(LocalDate.of(2020, 1, 1)).build());
        TeamNameHistory first = team.rename(new TeamName("SKT", "SK telecom T1", null, null, null),
                LocalDate.of(2021, 1, 1), null, null);
        teamPersistencePort.saveNameHistory(first);
        TeamNameHistory overlapping = TeamNameHistory.builder().teamId("t1").teamCode("SKT2").name("SKT T1 K")
                .validFrom(LocalDate.of(2020, 6, 1)).validTo(LocalDate.of(2020, 12, 1)).build();

        assertThatThrownBy(() -> teamPersistencePort.saveNameHistory(overlapping))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONTRACT_PERIOD_CONFLICT);
    }

    @DisplayName("하위 팀(parent_team_id 로 가리키는 팀)이 있으면 true")
    @Test
    void hasChildTeams_하위팀이_있으면_true() {
        teamPersistencePort.save(Team.builder().teamId("t1").teamCode("T1").gameCode("lol").name("T1").build());
        teamPersistencePort.save(Team.builder().teamId("t1a").teamCode("T1A").gameCode("lol").name("T1A")
                .parentTeamId("t1").build());

        assertThat(teamPersistencePort.hasChildTeams("t1")).isTrue();
        assertThat(teamPersistencePort.hasChildTeams("t1a")).isFalse();
    }

    @DisplayName("해체일 이후까지 이어지는 계약이 있으면 true")
    @Test
    void hasContractEffectiveOnOrAfter_해체일이후_계약이_있으면_true() {
        sql("""
                INSERT INTO esports_team (team_id, team_code, name) VALUES ('t1', 'T1', 'T1');
                INSERT INTO person (person_id, nick_name) VALUES ('faker', 'Faker');
                INSERT INTO esports_contract (person_id, team_id, role, position, contract_type, start_date, end_date)
                VALUES ('faker', 't1', 'PLAYER', 'MID', 'SIGN', '2025-11-18', '2027-11-15');
                """);

        assertThat(teamPersistencePort.hasContractEffectiveOnOrAfter("t1", LocalDate.of(2027, 11, 15))).isTrue();
        assertThat(teamPersistencePort.hasContractEffectiveOnOrAfter("t1", LocalDate.of(2027, 11, 16))).isFalse();
    }
}
