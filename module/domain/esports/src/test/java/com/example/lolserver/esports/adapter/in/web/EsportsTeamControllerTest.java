package com.example.lolserver.esports.adapter.in.web;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.esports.application.model.readmodel.LoanedOutReadModel;
import com.example.lolserver.esports.application.model.readmodel.RosterEntryReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamDetailReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamMemberReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamNameReadModel;
import com.example.lolserver.esports.application.model.readmodel.TeamRosterReadModel;
import com.example.lolserver.esports.application.port.in.EsportsQueryUseCase;
import com.example.lolserver.esports.domain.vo.ContractType;
import com.example.lolserver.esports.domain.vo.Position;
import com.example.lolserver.esports.domain.vo.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static com.example.lolserver.esports.adapter.in.web.EsportsDocs.doc;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.T1;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.T1A;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.T1_TENURE;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("EsportsTeamController 테스트")
@ExtendWith(MockitoExtension.class)
class EsportsTeamControllerTest extends RestDocsSupport {

    @Mock
    private EsportsQueryUseCase esportsQueryUseCase;

    @Override
    protected Object initController() {
        return new EsportsTeamController(esportsQueryUseCase);
    }

    @DisplayName("팀 목록 API — 기본은 해체 팀 제외")
    @Test
    void getTeams_200() throws Exception {
        given(esportsQueryUseCase.getTeams("lck", false)).willReturn(List.of(T1));

        mockMvc.perform(get("/api/v1/esports/teams").param("leagueId", "lck"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamId").value("t1"))
                .andDo(doc("esports-team-list"));
    }

    @DisplayName("팀 상세 API — 지나간 이름과 같은 조직의 팀")
    @Test
    void getTeamDetail_200() throws Exception {
        given(esportsQueryUseCase.getTeamDetail("t1")).willReturn(new TeamDetailReadModel(T1,
                List.of(new TeamNameReadModel(1L, "SKT", "SK telecom T1", null, null, null,
                        LocalDate.of(2013, 2, 1), LocalDate.of(2019, 10, 24))),
                List.of(T1A)));

        mockMvc.perform(get("/api/v1/esports/teams/{teamId}", "t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.team.teamCode").value("T1"))
                .andExpect(jsonPath("$.data.pastNames[0].teamCode").value("SKT"))
                .andExpect(jsonPath("$.data.organizationTeams[0].teamId").value("t1a"))
                .andDo(doc("esports-team-detail"));
    }

    @DisplayName("현재 로스터 API — 선수·스태프·임대 보낸 선수")
    @Test
    void getCurrentRoster_200() throws Exception {
        given(esportsQueryUseCase.getCurrentRoster("t1")).willReturn(new TeamRosterReadModel("t1",
                List.of(new RosterEntryReadModel("faker", "Faker", null, "KR", Role.PLAYER, Position.MID,
                        ContractType.RENEWAL, LocalDate.of(2026, 11, 17), LocalDate.of(2029, 11, 19), false, null,
                        false, null)),
                List.of(new RosterEntryReadModel("kkoma", "kkOma", null, "KR", Role.HEAD_COACH, null,
                        ContractType.SIGN, LocalDate.of(2026, 11, 17), LocalDate.of(2027, 11, 15), false, null,
                        false, null)),
                List.of(new LoanedOutReadModel("rookie", "Rookie", Position.TOP, "gen", LocalDate.of(2027, 6, 30)))));

        mockMvc.perform(get("/api/v1/esports/teams/{teamId}/roster", "t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.players[0].personId").value("faker"))
                .andExpect(jsonPath("$.data.staff[0].role").value("HEAD_COACH"))
                .andExpect(jsonPath("$.data.loanedOut[0].loanTeamId").value("gen"))
                .andDo(doc("esports-team-roster"));
    }

    @DisplayName("역대 인원 API — 이 팀을 거친 사람의 소속 기간")
    @Test
    void getTeamMembers_200() throws Exception {
        given(esportsQueryUseCase.getTeamMembers("t1")).willReturn(List.of(
                new TeamMemberReadModel(T1_TENURE, LocalDate.of(2013, 2, 1), null)));

        mockMvc.perform(get("/api/v1/esports/teams/{teamId}/members", "t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tenure.organizationId").value("t1"))
                .andExpect(jsonPath("$.data[0].teamStartDate").value("2013-02-01"))
                .andDo(doc("esports-team-members"));
    }
}
