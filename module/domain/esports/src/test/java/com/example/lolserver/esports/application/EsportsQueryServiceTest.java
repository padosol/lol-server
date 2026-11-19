package com.example.lolserver.esports.application;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.esports.application.port.out.EsportsQueryPort;
import com.example.lolserver.esports.application.port.out.LeaguePersistencePort;
import com.example.lolserver.esports.application.port.out.TeamPersistencePort;
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
class EsportsQueryServiceTest {

    @Mock
    private EsportsQueryPort esportsQueryPort;

    @Mock
    private LeaguePersistencePort leaguePersistencePort;

    @Mock
    private TeamPersistencePort teamPersistencePort;

    @InjectMocks
    private EsportsQueryService service;

    @DisplayName("없는 팀의 상세를 찾으면 404")
    @Test
    void getTeamDetail_없으면_ESPORTS_TEAM_NOT_FOUND() {
        given(esportsQueryPort.findTeamDetail("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTeamDetail("nope"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.ESPORTS_TEAM_NOT_FOUND);
    }

    @DisplayName("없는 팀의 로스터를 찾으면 조회하지 않고 404")
    @Test
    void getCurrentRoster_없는팀이면_ESPORTS_TEAM_NOT_FOUND() {
        given(teamPersistencePort.exists("nope")).willReturn(false);

        assertThatThrownBy(() -> service.getCurrentRoster("nope"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.ESPORTS_TEAM_NOT_FOUND);
        verify(esportsQueryPort, never()).findCurrentRoster(any());
    }

    @DisplayName("연결되지 않았거나 비공개인 소환사로 찾으면 404")
    @Test
    void getPersonByPuuid_연결없으면_PERSON_NOT_FOUND() {
        given(esportsQueryPort.findPersonByPuuid("puuid-x")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPersonByPuuid("puuid-x"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.PERSON_NOT_FOUND);
    }
}
