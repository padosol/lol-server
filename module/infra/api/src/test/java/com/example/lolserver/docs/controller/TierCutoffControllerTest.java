package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.tiercutoff.TierCutoffController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.tiercutoff.application.TierCutoffService;
import com.example.lolserver.domain.tiercutoff.application.model.TierCutoffReadModel;
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

@DisplayName("TierCutoffController 테스트")
@ExtendWith(MockitoExtension.class)
class TierCutoffControllerTest extends RestDocsSupport {

    @Mock
    private TierCutoffService tierCutoffService;

    @Override
    protected Object initController() {
        return new TierCutoffController(tierCutoffService);
    }

    @DisplayName("지역별 티어 컷오프 목록 조회 API")
    @Test
    void getTierCutoffs_성공() throws Exception {
        // given
        String region = "kr";
        List<TierCutoffReadModel> tierCutoffs = List.of(
                new TierCutoffReadModel(1L, "RANKED_SOLO_5x5", "CHALLENGER", "KR", 1500, "2026-01-15T12:00:00"),
                new TierCutoffReadModel(2L, "RANKED_SOLO_5x5", "GRANDMASTER", "KR", 800, "2026-01-15T12:00:00"),
                new TierCutoffReadModel(3L, "RANKED_FLEX_SR", "CHALLENGER", "KR", 1200, "2026-01-15T12:00:00"),
                new TierCutoffReadModel(4L, "RANKED_FLEX_SR", "GRANDMASTER", "KR", 600, "2026-01-15T12:00:00")
        );

        given(tierCutoffService.getTierCutoffsByRegion(region)).willReturn(tierCutoffs);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{region}/tier-cutoffs", region)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("tier-cutoff-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("서버 지역 (예: kr, na, euw)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("티어 컷오프 목록"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("티어 컷오프 ID"),
                                fieldWithPath("data[].queue").type(JsonFieldType.STRING)
                                        .description("큐 타입 (예: RANKED_SOLO_5x5, RANKED_FLEX_SR)"),
                                fieldWithPath("data[].tier").type(JsonFieldType.STRING)
                                        .description("티어 (CHALLENGER, GRANDMASTER)"),
                                fieldWithPath("data[].region").type(JsonFieldType.STRING)
                                        .description("서버 지역"),
                                fieldWithPath("data[].minLeaguePoints").type(JsonFieldType.NUMBER)
                                        .description("해당 티어 진입에 필요한 최소 리그 포인트"),
                                fieldWithPath("data[].updatedAt").type(JsonFieldType.STRING)
                                        .description("데이터 갱신 일시")
                        )
                ));
    }

    @DisplayName("지역별 큐 타입 필터 티어 컷오프 목록 조회 API")
    @Test
    void getTierCutoffsByQueue_성공() throws Exception {
        // given
        String region = "kr";
        String queue = "RANKED_SOLO_5x5";
        List<TierCutoffReadModel> tierCutoffs = List.of(
                new TierCutoffReadModel(1L, "RANKED_SOLO_5x5", "CHALLENGER", "KR", 1500, "2026-01-15T12:00:00"),
                new TierCutoffReadModel(2L, "RANKED_SOLO_5x5", "GRANDMASTER", "KR", 800, "2026-01-15T12:00:00")
        );

        given(tierCutoffService.getTierCutoffsByRegionAndQueue(region, queue)).willReturn(tierCutoffs);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{region}/tier-cutoffs", region)
                                .param("queue", queue)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("tier-cutoff-list-by-queue",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("서버 지역 (예: kr, na, euw)")
                        ),
                        queryParameters(
                                parameterWithName("queue").description("큐 타입 필터 (예: RANKED_SOLO_5x5, RANKED_FLEX_SR)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("티어 컷오프 목록"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("티어 컷오프 ID"),
                                fieldWithPath("data[].queue").type(JsonFieldType.STRING)
                                        .description("큐 타입 (예: RANKED_SOLO_5x5, RANKED_FLEX_SR)"),
                                fieldWithPath("data[].tier").type(JsonFieldType.STRING)
                                        .description("티어 (CHALLENGER, GRANDMASTER)"),
                                fieldWithPath("data[].region").type(JsonFieldType.STRING)
                                        .description("서버 지역"),
                                fieldWithPath("data[].minLeaguePoints").type(JsonFieldType.NUMBER)
                                        .description("해당 티어 진입에 필요한 최소 리그 포인트"),
                                fieldWithPath("data[].updatedAt").type(JsonFieldType.STRING)
                                        .description("데이터 갱신 일시")
                        )
                ));
    }

    @DisplayName("특정 티어 컷오프 상세 조회 API")
    @Test
    void getTierCutoff_성공() throws Exception {
        // given
        String region = "kr";
        String queue = "RANKED_SOLO_5x5";
        String tier = "CHALLENGER";
        TierCutoffReadModel tierCutoff = new TierCutoffReadModel(
                1L, "RANKED_SOLO_5x5", "CHALLENGER", "KR", 1500, "2026-01-15T12:00:00"
        );

        given(tierCutoffService.getTierCutoff(region, queue, tier)).willReturn(tierCutoff);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{region}/tier-cutoffs/{queue}/{tier}", region, queue, tier)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("tier-cutoff-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("서버 지역 (예: kr, na, euw)"),
                                parameterWithName("queue").description("큐 타입 (예: RANKED_SOLO_5x5, RANKED_FLEX_SR)"),
                                parameterWithName("tier").description("티어 (CHALLENGER, GRANDMASTER)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("티어 컷오프 ID"),
                                fieldWithPath("data.queue").type(JsonFieldType.STRING)
                                        .description("큐 타입 (예: RANKED_SOLO_5x5, RANKED_FLEX_SR)"),
                                fieldWithPath("data.tier").type(JsonFieldType.STRING)
                                        .description("티어 (CHALLENGER, GRANDMASTER)"),
                                fieldWithPath("data.region").type(JsonFieldType.STRING)
                                        .description("서버 지역"),
                                fieldWithPath("data.minLeaguePoints").type(JsonFieldType.NUMBER)
                                        .description("해당 티어 진입에 필요한 최소 리그 포인트"),
                                fieldWithPath("data.updatedAt").type(JsonFieldType.STRING)
                                        .description("데이터 갱신 일시")
                        )
                ));
    }
}
