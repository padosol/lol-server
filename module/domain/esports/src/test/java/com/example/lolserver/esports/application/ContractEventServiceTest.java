package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.ContractCommand;
import com.example.lolserver.esports.application.command.DatedCommand;
import com.example.lolserver.esports.application.model.readmodel.PersonStatusReadModel;
import com.example.lolserver.esports.application.model.resultmodel.ContractEventResultModel;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.application.port.out.PersonTimelinePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
import com.example.lolserver.esports.domain.Contract;
import com.example.lolserver.esports.domain.PersonTimeline;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContractEventServiceTest {

    @Mock
    private PersonPersistencePort personPersistencePort;

    @Mock
    private TeamPersistencePort teamPersistencePort;

    @Mock
    private PersonTimelinePort personTimelinePort;

    @Mock
    private EsportsQueryPort esportsQueryPort;

    @InjectMocks
    private ContractEventService service;

    private static final Team T1 = Team.builder().teamId("t1").teamCode("T1").gameCode("lol").name("T1").build();
    private static final Team GEN = Team.builder().teamId("gen").teamCode("GEN").gameCode("lol").name("Gen.G").build();

    @DisplayName("없는 팀으로 이적하면 타임라인을 읽지 않고 404")
    @Test
    void transfer_없는팀이면_ESPORTS_TEAM_NOT_FOUND() {
        given(personPersistencePort.exists("faker")).willReturn(true);
        given(teamPersistencePort.findById("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer("faker", command("nope")))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.ESPORTS_TEAM_NOT_FOUND);
        verify(personTimelinePort, never()).load(any());
    }

    @DisplayName("이적은 타임라인을 읽고 → 저장하고 → 저장 후 상태를 돌려준다")
    @Test
    void transfer_타임라인을_저장하고_상태를_돌려준다() {
        Contract current = Contract.builder().contractId(1L).personId("faker").teamId("t1").organizationId("t1")
                .role(Role.PLAYER).position(Position.MID).contractType(ContractType.SIGN)
                .startDate(LocalDate.of(2025, 11, 18)).endDate(LocalDate.of(2027, 11, 15)).build();
        PersonTimeline timeline = new PersonTimeline("faker", new ArrayList<>(List.of(current)), new ArrayList<>());
        PersonStatusReadModel status = new PersonStatusReadModel(PersonStatus.CONTRACTED, "gen", "gen", Role.PLAYER,
                Position.MID, LocalDate.of(2028, 11, 20), false, null, false, null, null, null, null);
        given(personPersistencePort.exists("faker")).willReturn(true);
        given(teamPersistencePort.findById("gen")).willReturn(Optional.of(GEN));
        given(personTimelinePort.load("faker")).willReturn(timeline);
        given(personTimelinePort.save(timeline)).willReturn(timeline);
        given(esportsQueryPort.findContractsByIds(anyCollection())).willReturn(List.of());
        given(esportsQueryPort.findPersonStatus("faker")).willReturn(Optional.of(status));

        ContractEventResultModel result = service.transfer("faker", command("gen"));

        InOrder order = inOrder(personTimelinePort, esportsQueryPort);
        order.verify(personTimelinePort).load("faker");
        order.verify(personTimelinePort).save(timeline);
        order.verify(esportsQueryPort).findPersonStatus("faker");
        assertThat(result.personId()).isEqualTo("faker");
        assertThat(result.status()).isEqualTo(status);
        assertThat(timeline.newContracts()).singleElement()
                .satisfies(c -> assertThat(c.getContractType()).isEqualTo(ContractType.TRANSFER));
    }

    @DisplayName("없는 인물의 은퇴는 404")
    @Test
    void retire_없는인물이면_PERSON_NOT_FOUND() {
        given(personPersistencePort.exists("nobody")).willReturn(false);

        assertThatThrownBy(() -> service.retire("nobody", new DatedCommand(LocalDate.of(2026, 11, 20), null, null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.PERSON_NOT_FOUND);
        verify(personTimelinePort, never()).load(any());
    }

    private static ContractCommand command(String teamId) {
        return new ContractCommand(teamId, Role.PLAYER, Position.MID, LocalDate.of(2026, 11, 20),
                LocalDate.of(2028, 11, 20), null);
    }
}
