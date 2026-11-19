package com.example.lolserver.esports.adapter.in.web;

import com.example.lolserver.common.error.CoreException;
import com.example.lolserver.common.error.ErrorType;
import com.example.lolserver.common.support.PageResult;
import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.common.web.CoreExceptionAdvice;
import com.example.lolserver.esports.application.command.PersonSearchCommand;
import com.example.lolserver.esports.application.model.readmodel.PersonCareerReadModel;
import com.example.lolserver.esports.application.model.readmodel.PersonProfileReadModel;
import com.example.lolserver.esports.application.model.readmodel.SummonerLinkReadModel;
import com.example.lolserver.esports.application.port.in.EsportsQueryUseCase;
import com.example.lolserver.esports.domain.vo.PersonStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static com.example.lolserver.esports.adapter.in.web.EsportsDocs.doc;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.CONTRACTED;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.EXPIRED;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.FAKER;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.HIATUS;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.RENEWAL;
import static com.example.lolserver.esports.adapter.in.web.EsportsReadModelFixtures.T1_TENURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("EsportsPersonController 테스트")
@ExtendWith(MockitoExtension.class)
class EsportsPersonControllerTest extends RestDocsSupport {

    @Mock
    private EsportsQueryUseCase esportsQueryUseCase;

    @Override
    protected Object initController() {
        return new EsportsPersonController(esportsQueryUseCase);
    }

    @DisplayName("인물 검색 API — 스토브리그 만료 예정자")
    @Test
    void searchPersons_200() throws Exception {
        given(esportsQueryUseCase.searchPersons(org.mockito.ArgumentMatchers.any())).willReturn(
                new PageResult<>(List.of(FAKER), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/esports/persons")
                        .param("status", "CONTRACTED")
                        .param("role", "PLAYER")
                        .param("contractEndingBy", "2027-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].personId").value("faker"))
                .andExpect(jsonPath("$.data.content[0].status.status").value("CONTRACTED"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andDo(doc("esports-person-search"));

        ArgumentCaptor<PersonSearchCommand> command = ArgumentCaptor.forClass(PersonSearchCommand.class);
        verify(esportsQueryUseCase).searchPersons(command.capture());
        assertThat(command.getValue().status()).isEqualTo(PersonStatus.CONTRACTED);
        assertThat(command.getValue().contractEndingBy()).isEqualTo(LocalDate.of(2027, 11, 15));
        assertThat(command.getValue().page()).isZero();
        assertThat(command.getValue().size()).isEqualTo(20);
    }

    @DisplayName("인물 프로필 API — 공개 소환사 puuid 포함")
    @Test
    void getPersonProfile_200() throws Exception {
        given(esportsQueryUseCase.getPersonProfile("faker")).willReturn(new PersonProfileReadModel("faker",
                "Faker", "이상혁", "Lee Sang-hyeok", LocalDate.of(1996, 5, 7), "KR", null, CONTRACTED,
                List.of(new SummonerLinkReadModel("puuid-main", true, true, null))));

        mockMvc.perform(get("/api/v1/esports/persons/{personId}", "faker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.status").value("CONTRACTED"))
                .andExpect(jsonPath("$.data.summoners[0].puuid").value("puuid-main"))
                .andDo(doc("esports-person-profile"));
    }

    @DisplayName("인물 커리어 API — 소속 기간·계약·사건")
    @Test
    void getPersonCareer_200() throws Exception {
        given(esportsQueryUseCase.getPersonCareer("faker")).willReturn(new PersonCareerReadModel("faker",
                List.of(T1_TENURE), List.of(EXPIRED, RENEWAL), List.of(HIATUS)));

        mockMvc.perform(get("/api/v1/esports/persons/{personId}/career", "faker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenures[0].renewalCount").value(5))
                .andExpect(jsonPath("$.data.contracts[0].endReason").value("RENEWED"))
                .andExpect(jsonPath("$.data.events[0].eventType").value("HIATUS"))
                .andDo(doc("esports-person-career"));
    }

    @DisplayName("소환사 → 인물 API — 전적 검색 화면의 프로 선수 배지")
    @Test
    void getPersonByPuuid_200() throws Exception {
        given(esportsQueryUseCase.getPersonByPuuid("puuid-main")).willReturn(FAKER);

        mockMvc.perform(get("/api/v1/esports/summoners/{puuid}/person", "puuid-main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamCode").value("T1"))
                .andExpect(jsonPath("$.data.nickName").value("Faker"))
                .andDo(doc("esports-summoner-person"));
    }

    @DisplayName("소환사 → 인물 API — 연결 없거나 비공개면 404")
    @Test
    void getPersonByPuuid_연결없으면_404() throws Exception {
        given(esportsQueryUseCase.getPersonByPuuid("puuid-x")).willThrow(new CoreException(ErrorType.PERSON_NOT_FOUND));
        MockMvc withAdvice = MockMvcBuilders.standaloneSetup(initController())
                .setControllerAdvice(new CoreExceptionAdvice())
                .build();

        withAdvice.perform(get("/api/v1/esports/summoners/{puuid}/person", "puuid-x"))
                .andExpect(status().isNotFound());
    }
}
