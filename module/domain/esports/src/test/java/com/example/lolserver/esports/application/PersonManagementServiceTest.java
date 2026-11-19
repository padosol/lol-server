package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.command.LinkSummonerCommand;
import com.example.lolserver.esports.application.port.out.PersonPersistencePort;
import com.example.lolserver.esports.domain.PersonSummoner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonManagementServiceTest {

    @Mock
    private PersonPersistencePort personPersistencePort;

    @InjectMocks
    private PersonManagementService service;

    @DisplayName("계약·사건·소환사 연결이 있는 인물은 삭제를 거부한다")
    @Test
    void deletePerson_기록이있으면_PERSON_IN_USE() {
        given(personPersistencePort.exists("faker")).willReturn(true);
        given(personPersistencePort.hasRecords("faker")).willReturn(true);

        assertThatThrownBy(() -> service.deletePerson("faker"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.PERSON_IN_USE);
        verify(personPersistencePort, never()).delete(any());
    }

    @DisplayName("다른 인물에 연결된 소환사 계정은 연결을 거부한다")
    @Test
    void linkSummoner_다른인물에_연결된_puuid면_SUMMONER_ALREADY_LINKED() {
        given(personPersistencePort.exists("faker")).willReturn(true);
        given(personPersistencePort.findSummoner("puuid-1")).willReturn(Optional.of(
                PersonSummoner.builder().puuid("puuid-1").personId("someone").visible(true).build()));

        assertThatThrownBy(() -> service.linkSummoner("faker", "puuid-1", new LinkSummonerCommand(true, true, null)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.SUMMONER_ALREADY_LINKED);
        verify(personPersistencePort, never()).saveSummoner(any());
    }
}
