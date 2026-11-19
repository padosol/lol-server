package com.example.lolserver.esports.adapter.in.web.admin;

import com.example.lolserver.common.test.RestDocsSupport;
import com.example.lolserver.esports.application.port.in.PersonManagementUseCase;
import com.example.lolserver.esports.domain.Person;
import com.example.lolserver.esports.domain.PersonSummoner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;

import static com.example.lolserver.esports.adapter.in.web.EsportsDocs.doc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminPersonController 테스트")
@ExtendWith(MockitoExtension.class)
class AdminPersonControllerTest extends RestDocsSupport {

    private static final Person FAKER = Person.builder().personId("faker").nickName("Faker").realName("이상혁")
            .realNameEng("Lee Sang-hyeok").birthDate(LocalDate.of(1996, 5, 7)).nationality("KR").build();
    private static final PersonSummoner MAIN = PersonSummoner.builder().puuid("puuid-main").personId("faker")
            .main(true).visible(true).note("본계정").build();

    @Mock
    private PersonManagementUseCase personManagementUseCase;

    @Override
    protected Object initController() {
        return new AdminPersonController(personManagementUseCase);
    }

    @DisplayName("인물 등록 API")
    @Test
    void createPerson_201() throws Exception {
        given(personManagementUseCase.createPerson(any())).willReturn(FAKER);

        mockMvc.perform(post("/api/v1/admin/esports/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personId": "faker", "nickName": "Faker", "realName": "이상혁",
                                 "realNameEng": "Lee Sang-hyeok", "birthDate": "1996-05-07", "nationality": "KR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.personId").value("faker"))
                .andExpect(jsonPath("$.data.birthDate").value("1996-05-07"))
                .andDo(doc("admin-esports-person-create"));
    }

    @DisplayName("인물 등록 — 국적이 두 글자 대문자가 아니면 400")
    @Test
    void createPerson_국적형식이_틀리면_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/esports/persons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"personId": "faker", "nickName": "Faker", "nationality": "KOR"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("인물 수정 API")
    @Test
    void updatePerson_200() throws Exception {
        given(personManagementUseCase.updatePerson(eq("faker"), any())).willReturn(FAKER);

        mockMvc.perform(put("/api/v1/admin/esports/persons/{personId}", "faker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickName": "Faker", "realName": "이상혁", "nationality": "KR"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickName").value("Faker"))
                .andDo(doc("admin-esports-person-update"));
    }

    @DisplayName("인물 삭제 API")
    @Test
    void deletePerson_204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/esports/persons/{personId}", "faker"))
                .andExpect(status().isNoContent())
                .andDo(doc("admin-esports-person-delete"));

        verify(personManagementUseCase).deletePerson("faker");
    }

    @DisplayName("소환사 연결 목록 API — 비공개 계정 포함")
    @Test
    void getSummoners_200() throws Exception {
        given(personManagementUseCase.getSummoners("faker")).willReturn(List.of(MAIN));

        mockMvc.perform(get("/api/v1/admin/esports/persons/{personId}/summoners", "faker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].puuid").value("puuid-main"))
                .andExpect(jsonPath("$.data[0].main").value(true))
                .andDo(doc("admin-esports-person-summoner-list"));
    }

    @DisplayName("소환사 연결 API — puuid 는 화면이 lol-repository 에서 얻어 넘긴다")
    @Test
    void linkSummoner_200() throws Exception {
        given(personManagementUseCase.linkSummoner(eq("faker"), eq("puuid-main"), any())).willReturn(MAIN);

        mockMvc.perform(put("/api/v1/admin/esports/persons/{personId}/summoners/{puuid}", "faker", "puuid-main")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"main": true, "visible": true, "note": "본계정"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.visible").value(true))
                .andDo(doc("admin-esports-person-summoner-link"));
    }

    @DisplayName("소환사 연결 해제 API")
    @Test
    void unlinkSummoner_204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/esports/persons/{personId}/summoners/{puuid}", "faker", "puuid-main"))
                .andExpect(status().isNoContent())
                .andDo(doc("admin-esports-person-summoner-unlink"));

        verify(personManagementUseCase).unlinkSummoner("faker", "puuid-main");
    }
}
