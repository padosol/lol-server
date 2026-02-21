package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.admin.AdminSummonerController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.summoner.application.SummonerService;
import com.example.lolserver.domain.summoner.application.dto.SummonerRenewalInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSummonerControllerTest extends RestDocsSupport {

    @Mock
    private SummonerService summonerService;

    @InjectMocks
    private AdminSummonerController adminSummonerController;

    @Override
    protected Object initController() {
        return adminSummonerController;
    }

    @Test
    @DisplayName("갱신 중인 소환사 목록 조회 API")
    void getRefreshingSummoners() throws Exception {
        // given
        List<SummonerRenewalInfoResponse> responses = List.of(
                SummonerRenewalInfoResponse.builder()
                        .puuid("puuid-1")
                        .gameName("Player1")
                        .tagLine("KR1")
                        .build(),
                SummonerRenewalInfoResponse.builder()
                        .puuid("puuid-2")
                        .gameName("Player2")
                        .tagLine("NA1")
                        .build()
        );
        given(summonerService.getRefreshingSummoners()).willReturn(responses);

        // when
        ResultActions result = mockMvc.perform(get("/api/admin/summoners/renewals"));

        // then
        result.andExpect(status().isOk())
                .andDo(document("admin-summoner-renewals",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data[].puuid").type(JsonFieldType.STRING).description("소환사 PUUID"),
                                fieldWithPath("data[].gameName").type(JsonFieldType.STRING).description("게임 유저명"),
                                fieldWithPath("data[].tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }

    @Test
    @DisplayName("갱신 중인 소환사가 없으면 빈 리스트를 반환한다")
    void getRefreshingSummoners_empty() throws Exception {
        // given
        given(summonerService.getRefreshingSummoners()).willReturn(Collections.emptyList());

        // when
        ResultActions result = mockMvc.perform(get("/api/admin/summoners/renewals"));

        // then
        result.andExpect(status().isOk())
                .andDo(document("admin-summoner-renewals-empty",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 성공 여부"),
                                fieldWithPath("data").type(JsonFieldType.ARRAY).description("갱신 중인 소환사 목록 (비어있음)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 정보 (정상 응답 시 null)")
                        )
                ));
    }
}
