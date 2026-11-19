package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.esports.application.port.in.EsportsMasterUseCase;
import com.example.lolserver.esports.domain.League;
import com.example.lolserver.esports.domain.Season;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminLeagueController 테스트")
@ExtendWith(MockitoExtension.class)
class AdminLeagueControllerTest extends RestDocsSupport {

    private static final League LCK = League.builder().leagueId("lck").gameCode("lol").name("LCK")
            .nameAcronym("LCK").region("KR").sortOrder(10).active(true).build();
    private static final Season LCK_2026 = Season.builder().seasonId("lck_2026").leagueId("lck").name("2026 LCK")
            .year(2026).startDate(LocalDate.of(2026, 1, 14)).endDate(LocalDate.of(2026, 9, 6)).build();

    @Mock
    private EsportsMasterUseCase esportsMasterUseCase;

    @Override
    protected Object initController() {
        return new AdminLeagueController(esportsMasterUseCase);
    }

    @DisplayName("리그 등록 API")
    @Test
    void createLeague_201() throws Exception {
        given(esportsMasterUseCase.createLeague(any())).willReturn(LCK);

        mockMvc.perform(post("/api/v1/admin/esports/leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leagueId": "lck", "name": "LCK", "nameAcronym": "LCK", "region": "KR",
                                 "international": false, "sortOrder": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.leagueId").value("lck"))
                .andDo(doc("admin-esports-league-create"));
    }

    @DisplayName("리그 등록 — ID 형식이 틀리면 400")
    @Test
    void createLeague_ID형식이_틀리면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/esports/leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"leagueId": "LCK!", "name": "LCK"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("리그 수정 API")
    @Test
    void updateLeague_200() throws Exception {
        given(esportsMasterUseCase.updateLeague(eq("lck"), any())).willReturn(LCK);

        mockMvc.perform(put("/api/v1/admin/esports/leagues/{leagueId}", "lck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "LCK", "nameAcronym": "LCK", "region": "KR", "sortOrder": 10, "active": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("LCK"))
                .andDo(doc("admin-esports-league-update"));
    }

    @DisplayName("시즌 등록 API")
    @Test
    void createSeason_201() throws Exception {
        given(esportsMasterUseCase.createSeason(eq("lck"), any())).willReturn(LCK_2026);

        mockMvc.perform(post("/api/v1/admin/esports/leagues/{leagueId}/seasons", "lck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seasonId": "lck_2026", "name": "2026 LCK", "year": 2026,
                                 "startDate": "2026-01-14", "endDate": "2026-09-06"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seasonId").value("lck_2026"))
                .andExpect(jsonPath("$.data.startDate").value("2026-01-14"))
                .andDo(doc("admin-esports-season-create"));
    }

    @DisplayName("시즌 수정 API")
    @Test
    void updateSeason_200() throws Exception {
        given(esportsMasterUseCase.updateSeason(eq("lck_2026"), any())).willReturn(LCK_2026);

        mockMvc.perform(put("/api/v1/admin/esports/seasons/{seasonId}", "lck_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "2026 LCK", "year": 2026, "startDate": "2026-01-14", "endDate": "2026-09-06"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leagueId").value("lck"))
                .andDo(doc("admin-esports-season-update"));
    }
}
