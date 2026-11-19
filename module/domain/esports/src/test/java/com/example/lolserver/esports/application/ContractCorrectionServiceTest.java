package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.CorrectContractCommand;
import com.example.lolserver.esports.application.model.readmodel.ContractReadModel;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.application.port.out.PersonTimelinePort;
import com.example.lolserver.esports.domain.Contract;
import com.example.lolserver.esports.domain.vo.ContractStatus;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.EndReason;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContractCorrectionServiceTest {

    @Mock
    private PersonPersistencePort personPersistencePort;

    @Mock
    private PersonTimelinePort personTimelinePort;

    @Mock
    private EsportsQueryPort esportsQueryPort;

    @InjectMocks
    private ContractCorrectionService service;

    @DisplayName("계약을 정정해 저장한다 (사건 규칙은 거치지 않는다)")
    @Test
    void correctContract_계약을_정정해_저장한다() {
        Contract contract = Contract.builder().contractId(7L).personId("faker").teamId("t1").organizationId("t1")
                .role(Role.PLAYER).position(Position.MID).contractType(ContractType.SIGN)
                .startDate(LocalDate.of(2025, 11, 18)).endDate(LocalDate.of(2027, 11, 15)).build();
        given(personTimelinePort.findContract(7L)).willReturn(Optional.of(contract));
        given(personTimelinePort.saveContract(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(esportsQueryPort.findContractsByIds(List.of(7L))).willReturn(List.of(new ContractReadModel(7L, "t1",
                "T1", Role.PLAYER, Position.MID, ContractType.SIGN, LocalDate.of(2025, 11, 18),
                LocalDate.of(2027, 11, 15), LocalDate.of(2026, 5, 31), LocalDate.of(2026, 5, 31), ContractStatus.ENDED,
                EndReason.RELEASED, "정정")));

        service.correctContract(7L, new CorrectContractCommand(LocalDate.of(2025, 11, 18),
                LocalDate.of(2027, 11, 15), LocalDate.of(2026, 5, 31), EndReason.RELEASED, Position.MID, "정정"));

        ArgumentCaptor<Contract> saved = ArgumentCaptor.forClass(Contract.class);
        verify(personTimelinePort).saveContract(saved.capture());
        assertThat(saved.getValue().getActualEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(saved.getValue().getEndReason()).isEqualTo(EndReason.RELEASED);
    }

    @DisplayName("없는 계약을 정정하면 404")
    @Test
    void correctContract_없으면_CONTRACT_NOT_FOUND() {
        given(personTimelinePort.findContract(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.correctContract(99L, new CorrectContractCommand(LocalDate.of(2025, 11, 18),
                null, null, null, Position.MID, null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONTRACT_NOT_FOUND);
        verify(personTimelinePort, never()).saveContract(any());
    }

    @DisplayName("없는 커리어 사건을 지우면 404")
    @Test
    void deleteCareerEvent_없으면_CAREER_EVENT_NOT_FOUND() {
        given(personTimelinePort.findEvent(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCareerEvent(5L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CAREER_EVENT_NOT_FOUND);
        verify(personTimelinePort, never()).deleteEvent(any());
    }
}
