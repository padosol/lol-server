package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.season.SeasonController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.season.application.SeasonService;
import com.example.lolserver.domain.season.application.model.SeasonReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SeasonController 테스트")
@ExtendWith(MockitoExtension.class)
class SeasonControllerTest extends RestDocsSupport {

    @Mock
    private SeasonService seasonService;

    @Override
    protected Object initController() {
        return new SeasonController(seasonService);
    }

    @DisplayName("전체 시즌 목록 조회 API")
    @Test
    void getAllSeasons_성공() throws Exception {
        // given
        List<SeasonReadModel> seasons = List.of(
                new SeasonReadModel(15, "2025 Season 1", List.of("25.S1.3", "25.S1.2")),
                new SeasonReadModel(14, "2024 Season 3", List.of("14.24.1"))
        );

        given(seasonService.getAllSeasons()).willReturn(seasons);

        // when & then
        mockMvc.perform(
                        get("/api/v1/seasons")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("season-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("시즌 목록 (최신순 정렬)"),
                                fieldWithPath("data[].seasonValue").type(JsonFieldType.NUMBER)
                                        .description("시즌 번호"),
                                fieldWithPath("data[].seasonName").type(JsonFieldType.STRING)
                                        .description("시즌 이름"),
                                fieldWithPath("data[].patchVersions[]").type(JsonFieldType.ARRAY)
                                        .description("해당 시즌의 패치 버전 목록 (e.g., [\"25.S1.3\", \"25.S1.2\"])")
                        )
                ));
    }

    @DisplayName("특정 시즌 조회 API - 시즌 존재")
    @Test
    void getSeasonById_시즌존재_성공() throws Exception {
        // given
        Long seasonId = 1L;
        SeasonReadModel season = new SeasonReadModel(
                15, "2025 Season 1", List.of("25.S1.3", "25.S1.2")
        );

        given(seasonService.getSeasonById(seasonId)).willReturn(season);

        // when & then
        mockMvc.perform(
                        get("/api/v1/seasons/{seasonId}", seasonId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("season-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("seasonId").description("조회할 시즌 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.seasonValue").type(JsonFieldType.NUMBER)
                                        .description("시즌 번호"),
                                fieldWithPath("data.seasonName").type(JsonFieldType.STRING)
                                        .description("시즌 이름"),
                                fieldWithPath("data.patchVersions[]").type(JsonFieldType.ARRAY)
                                        .description("해당 시즌의 패치 버전 목록 (e.g., [\"25.S1.3\", \"25.S1.2\"])")
                        )
                ));
    }
}
