package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.CreateTeamCommand;
import com.example.lolserver.esports.application.command.RenameTeamCommand;
import com.example.lolserver.esports.application.port.out.LeaguePersistencePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamName;
import com.example.lolserver.esports.domain.TeamNameHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EsportsMasterServiceTest {

    @Mock
    private LeaguePersistencePort leaguePersistencePort;

    @Mock
    private TeamPersistencePort teamPersistencePort;

    @InjectMocks
    private EsportsMasterService service;

    @DisplayName("이미 있는 팀 ID 로 등록하면 거부한다")
    @Test
    void createTeam_ID중복이면_ESPORTS_ID_DUPLICATED() {
        given(teamPersistencePort.exists("t1")).willReturn(true);

        assertThatThrownBy(() -> service.createTeam(teamCommand("t1", null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.ESPORTS_ID_DUPLICATED);
        verify(teamPersistencePort, never()).save(any());
    }

    @DisplayName("상위 팀에 또 상위 팀이 있으면 등록을 거부한다")
    @Test
    void createTeam_상위팀에_상위팀이_있으면_TEAM_NESTED_TOO_DEEP() {
        given(teamPersistencePort.exists("x")).willReturn(false);
        given(teamPersistencePort.findById("t1a")).willReturn(Optional.of(
                Team.builder().teamId("t1a").teamCode("T1A").gameCode("lol").name("T1A").parentTeamId("t1").build()));

        assertThatThrownBy(() -> service.createTeam(teamCommand("x", "t1a")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.TEAM_NESTED_TOO_DEEP);
    }

    @DisplayName("개명은 지나간 이름을 이력으로 저장한 뒤 팀을 새 이름으로 저장한다")
    @Test
    void renameTeam_이력을_저장하고_팀을_갱신한다() {
        Team t1 = Team.builder().teamId("t1").teamCode("SKT").gameCode("lol").name("SK telecom T1")
                .nameSince(LocalDate.of(2013, 2, 1)).build();
        given(teamPersistencePort.findById("t1")).willReturn(Optional.of(t1));
        given(teamPersistencePort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        RenameTeamCommand command = new RenameTeamCommand(LocalDate.of(2019, 10, 25),
                new TeamName("T1", "T1", "T1", null, null), null, "리브랜딩");

        Team renamed = service.renameTeam("t1", command);

        ArgumentCaptor<TeamNameHistory> history = ArgumentCaptor.forClass(TeamNameHistory.class);
        var order = inOrder(teamPersistencePort);
        order.verify(teamPersistencePort).saveNameHistory(history.capture());
        order.verify(teamPersistencePort).save(t1);
        assertThat(history.getValue().getName()).isEqualTo("SK telecom T1");
        assertThat(history.getValue().getValidTo()).isEqualTo(LocalDate.of(2019, 10, 24));
        assertThat(renamed.getName()).isEqualTo("T1");
        assertThat(renamed.getNameSince()).isEqualTo(LocalDate.of(2019, 10, 25));
    }

    @DisplayName("해체일 다음 날 이후까지 이어지는 계약이 있으면 해체를 거부한다")
    @Test
    void disbandTeam_해체일이후_계약이있으면_TEAM_HAS_ACTIVE_CONTRACT() {
        given(teamPersistencePort.findById("t1")).willReturn(Optional.of(
                Team.builder().teamId("t1").teamCode("T1").gameCode("lol").name("T1").build()));
        given(teamPersistencePort.hasContractEffectiveOnOrAfter("t1", LocalDate.of(2026, 11, 21))).willReturn(true);

        assertThatThrownBy(() -> service.disbandTeam("t1", LocalDate.of(2026, 11, 20)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.TEAM_HAS_ACTIVE_CONTRACT);
        verify(teamPersistencePort, never()).save(any());
    }

    private static CreateTeamCommand teamCommand(String teamId, String parentTeamId) {
        return new CreateTeamCommand(teamId, new TeamName(teamId.toUpperCase(), teamId, null, null, null),
                null, parentTeamId, null, null);
    }
}
