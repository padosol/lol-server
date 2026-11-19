package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.esports.application.port.in.EsportsMasterUseCase;
import com.example.lolserver.esports.domain.Team;
import com.example.lolserver.esports.domain.TeamNameHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static com.example.lolserver.esports.adapter.in.web.EsportsDocs.doc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminTeamController 테스트")
@ExtendWith(MockitoExtension.class)
class AdminTeamControllerTest extends RestDocsSupport {

    private static final Team T1A = Team.builder().teamId("t1a").teamCode("T1A").gameCode("lol")
            .name("T1 Esports Academy").homeLeagueId("lck_cl").parentTeamId("t1")
            .nameSince(LocalDate.of(2021, 1, 1)).build();
    private static final TeamNameHistory HISTORY = TeamNameHistory.builder().historyId(7L).teamId("t1")
            .teamCode("SKT").name("SK telecom T1").validFrom(LocalDate.of(2013, 2, 1))
            .validTo(LocalDate.of(2019, 10, 24)).build();

    @Mock
    private EsportsMasterUseCase esportsMasterUseCase;

    @Override
    protected Object initController() {
        return new AdminTeamController(esportsMasterUseCase);
    }

    @DisplayName("팀 등록 API — CL 팀은 상위 팀을 지정한다")
    @Test
    void createTeam_201() throws Exception {
        given(esportsMasterUseCase.createTeam(any())).willReturn(T1A);

        mockMvc.perform(post("/api/v1/admin/esports/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamId": "t1a", "teamCode": "T1A", "name": "T1 Esports Academy",
                                 "homeLeagueId": "lck_cl", "parentTeamId": "t1", "nameSince": "2021-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.teamId").value("t1a"))
                .andExpect(jsonPath("$.data.parentTeamId").value("t1"))
                .andDo(doc("admin-esports-team-create"));
    }

    @DisplayName("팀 등록 — 팀명이 없으면 400")
    @Test
    void createTeam_팀명이_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/esports/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamId": "t1a", "teamCode": "T1A"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("팀 수정 API — 이름·코드·로고는 개명·정정으로만 바꾼다")
    @Test
    void updateTeam_200() throws Exception {
        given(esportsMasterUseCase.updateTeam(eq("t1a"), any())).willReturn(T1A);

        mockMvc.perform(put("/api/v1/admin/esports/teams/{teamId}", "t1a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"homeLeagueId": "lck_cl", "parentTeamId": "t1", "foundedAt": "2021-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.homeLeagueId").value("lck_cl"))
                .andDo(doc("admin-esports-team-update"));
    }

    @DisplayName("개명 API — 현재 이름은 이력으로 넘어간다")
    @Test
    void renameTeam_200() throws Exception {
        given(esportsMasterUseCase.renameTeam(eq("t1a"), any())).willReturn(T1A);

        mockMvc.perform(post("/api/v1/admin/esports/teams/{teamId}/renames", "t1a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"renamedOn": "2027-01-01", "teamCode": "T1A", "name": "T1 Esports Academy", "note": "리브랜딩"}
                                """))
                .andExpect(status().isOk())
                .andDo(doc("admin-esports-team-rename"));
    }

    @DisplayName("현재 이름 정정 API — 이력을 남기지 않는다")
    @Test
    void correctCurrentName_200() throws Exception {
        given(esportsMasterUseCase.correctCurrentName(eq("t1a"), any())).willReturn(T1A);

        mockMvc.perform(patch("/api/v1/admin/esports/teams/{teamId}/current-name", "t1a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamCode": "T1A", "name": "T1 Esports Academy", "nameSince": "2021-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("T1 Esports Academy"))
                .andDo(doc("admin-esports-team-correct-name"));
    }

    @DisplayName("지나간 이름 정정 API")
    @Test
    void updateNameHistory_200() throws Exception {
        given(esportsMasterUseCase.updateNameHistory(eq("t1"), eq(7L), any())).willReturn(HISTORY);

        mockMvc.perform(put("/api/v1/admin/esports/teams/{teamId}/name-history/{historyId}", "t1", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teamCode": "SKT", "name": "SK telecom T1", "validFrom": "2013-02-01",
                                 "validTo": "2019-10-24"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.historyId").value(7))
                .andExpect(jsonPath("$.data.validTo").value("2019-10-24"))
                .andDo(doc("admin-esports-team-name-history-update"));
    }

    @DisplayName("지나간 이름 삭제 API")
    @Test
    void deleteNameHistory_204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/esports/teams/{teamId}/name-history/{historyId}", "t1", 7L))
                .andExpect(status().isNoContent())
                .andDo(doc("admin-esports-team-name-history-delete"));

        verify(esportsMasterUseCase).deleteNameHistory("t1", 7L);
    }

    @DisplayName("팀 해체 API")
    @Test
    void disbandTeam_200() throws Exception {
        given(esportsMasterUseCase.disbandTeam("t1a", LocalDate.of(2026, 11, 30))).willReturn(T1A);

        mockMvc.perform(post("/api/v1/admin/esports/teams/{teamId}/disbandment", "t1a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"disbandedAt": "2026-11-30"}
                                """))
                .andExpect(status().isOk())
                .andDo(doc("admin-esports-team-disband"));
    }
}
