package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.rank.RankController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.rank.dto.RankSearchDto;
import com.example.lolserver.domain.rank.service.RankService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RankControllerTest extends RestDocsSupport {

    @Mock
    private RankService rankService;

    @InjectMocks
    private RankController rankController;

    @Override
    protected Object initController() {
        return rankController;
    }

    @DisplayName("랭킹 조회 API")
    @Test
    void getSummonerRank() throws Exception {
        // given
        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setPlatform("kr");
        searchDto.setType(RankSearchDto.GameType.SOLO);
        searchDto.setPage(1);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> ranker = new HashMap<>();
        ranker.put("summonerName", "hide on bush");
        ranker.put("tagLine", "KR1");
        ranker.put("win", 100);
        ranker.put("losses", 50);
        ranker.put("point", 1234);
        ranker.put("tier", "CHALLENGER I");
        ranker.put("summonerLevel", 500L);
        ranker.put("position", "MID");
        ranker.put("championNames", List.of("Ahri", "Zed"));

        response.put("result", List.of(ranker));
        response.put("total", 1L);
        response.put("totalPage", 1L);

        given(rankService.getSummonerRank(any(RankSearchDto.class))).willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/rank")
                                .param("platform", searchDto.getPlatform())
                                .param("type", searchDto.getType().name())
                                .param("page", String.valueOf(searchDto.getPage()))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("rank-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("platform").description("플랫폼(지역)"),
                                parameterWithName("type").description("게임 타입 (SOLO, FLEX)").optional(),
                                parameterWithName("page").description("페이지 번호 (1부터 시작)").optional(),
                                parameterWithName("tier").description("조회할 티어 (e.g., CHALLENGER, GOLD)").optional()
                        ),
                        responseFields(
                                fieldWithPath("total").type(JsonFieldType.NUMBER).description("총 랭커 수"),
                                fieldWithPath("totalPage").type(JsonFieldType.NUMBER).description("총 페이지 수"),
                                fieldWithPath("result[].summonerName").type(JsonFieldType.STRING).description("소환사 명"),
                                fieldWithPath("result[].tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                fieldWithPath("result[].win").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("result[].losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("result[].point").type(JsonFieldType.NUMBER).description("리그 포인트(LP)"),
                                fieldWithPath("result[].tier").type(JsonFieldType.STRING).description("티어 및 디비전"),
                                fieldWithPath("result[].summonerLevel").type(JsonFieldType.NUMBER).description("소환사 레벨"),
                                fieldWithPath("result[].position").type(JsonFieldType.STRING).description("주 포지션").optional(),
                                fieldWithPath("result[].championNames[]").type(JsonFieldType.ARRAY).description("주요 챔피언 이름 목록").optional()
                        )
                ));
    }
}
