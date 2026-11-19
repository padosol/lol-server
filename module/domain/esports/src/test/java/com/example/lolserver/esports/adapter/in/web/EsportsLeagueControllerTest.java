package com.example.lolserver.esports.adapter.in.web;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.esports.application.model.readmodel.LeagueReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonReadModel;
import com.example.lolserver.esports.application.model.readmodel.SeasonRosterReadModel;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("EsportsLeagueController 테스트")
@ExtendWith(MockitoExtension.class)
class EsportsLeagueControllerTest extends RestDocsSupport {

    @Mock
    private EsportsQueryUseCase esportsQueryUseCase;

    @Override
    protected Object initController() {
        return new EsportsLeagueController(esportsQueryUseCase);
    }

    @DisplayName("리그 목록 API")
    @Test
    void getLeagues_200() throws Exception {
        given(esportsQueryUseCase.getLeagues()).willReturn(List.of(
                new LeagueReadModel("lck", "LCK", "LCK", "KR", false, null, null, 10, true),
                new LeagueReadModel("worlds", "Worlds", "WORLDS", null, true, null, null, 10, true)));

        mockMvc.perform(get("/api/v1/esports/leagues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].leagueId").value("lck"))
                .andExpect(jsonPath("$.data[1].international").value(true))
                .andDo(doc("esports-league-list"));
    }

    @DisplayName("시즌 목록 API")
    @Test
    void getSeasons_200() throws Exception {
        given(esportsQueryUseCase.getSeasons("lck")).willReturn(List.of(new SeasonReadModel("lck_2026", "lck",
                "2026 LCK", "LCK 2026", 2026, LocalDate.of(2026, 1, 14), LocalDate.of(2026, 9, 6), 0)));

        mockMvc.perform(get("/api/v1/esports/leagues/{leagueId}/seasons", "lck"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seasonId").value("lck_2026"))
                .andDo(doc("esports-season-list"));
    }

    @DisplayName("시즌 로스터 API — 팀명은 그 시즌 당시 이름")
    @Test
    void getSeasonRoster_200() throws Exception {
        given(esportsQueryUseCase.getSeasonRoster("lck_2019", "t1")).willReturn(List.of(new SeasonRosterReadModel(
                "lck_2019", "t1", "SKT", "SK telecom T1", "faker", "Faker", Role.PLAYER, Position.MID,
                ContractType.RENEWAL, LocalDate.of(2018, 11, 19), LocalDate.of(2019, 11, 18))));

        mockMvc.perform(get("/api/v1/esports/seasons/{seasonId}/rosters", "lck_2019").param("teamId", "t1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamCodeAtSeason").value("SKT"))
                .andExpect(jsonPath("$.data[0].position").value("MID"))
                .andDo(doc("esports-season-roster"));
    }
}
