package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.rank.RankController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.rank.application.RankService;
import com.example.lolserver.domain.rank.application.model.RankReadModel;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.domain.Rank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
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
        String platformId = "kr";

        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRankType(RankSearchDto.GameType.SOLO);
        searchDto.setPage(1);

        Rank rank = Rank.builder()
                .puuid("puuid-faker")
                .currentRank(1)
                .rankChange(0)
                .gameName("hide on bush")
                .tagLine("KR1")
                .wins(100)
                .losses(50)
                .winRate(new BigDecimal("66.67"))
                .tier("CHALLENGER")
                .rank(null)
                .leaguePoints(1234)
                .champions(List.of("Ahri", "Zed"))
                .build();

        List<RankReadModel> rankResponses = List.of(new RankReadModel(rank));
        Page<RankReadModel> response = new PageImpl<>(rankResponses, PageRequest.of(0, 50), 1);

        given(rankService.getRanks(any(RankSearchDto.class), eq(platformId))).willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{platformId}/rank", platformId)
                                .param("rankType", searchDto.getRankType().name())
                                .param("page", String.valueOf(searchDto.getPage()))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("rank-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("platformId").description("플랫폼 ID (e.g., kr)")
                        ),
                        queryParameters(
                                parameterWithName("rankType").description("게임 타입 (SOLO, FLEX)").optional(),
                                parameterWithName("page").description("페이지 번호 (1부터 시작, 페이지당 50개)").optional(),
                                parameterWithName("tier").description("조회할 티어 (e.g., CHALLENGER, GOLD)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.content[].puuid").type(JsonFieldType.STRING).description("소환사 PUUID"),
                                fieldWithPath("data.content[].currentRank").type(JsonFieldType.NUMBER).description("현재 순위"),
                                fieldWithPath("data.content[].rankChange").type(JsonFieldType.NUMBER).description("순위 변동 (양수: 상승, 음수: 하락)"),
                                fieldWithPath("data.content[].gameName").type(JsonFieldType.STRING).description("소환사 게임 이름"),
                                fieldWithPath("data.content[].tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                fieldWithPath("data.content[].wins").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("data.content[].losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("data.content[].winRate").type(JsonFieldType.NUMBER).description("승률 (%)"),
                                fieldWithPath("data.content[].tier").type(JsonFieldType.STRING).description("티어 (CHALLENGER, GRANDMASTER, MASTER, DIAMOND 등)"),
                                fieldWithPath("data.content[].rank").type(JsonFieldType.NULL).description("랭크 (I, II, III, IV 또는 null)").optional(),
                                fieldWithPath("data.content[].leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트(LP)"),
                                fieldWithPath("data.content[].champions").type(JsonFieldType.ARRAY).description("주요 챔피언 이름 목록"),
                                fieldWithPath("data.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호 (1부터 시작)"),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("페이지당 항목 수"),
                                fieldWithPath("data.totalElements").type(JsonFieldType.NUMBER).description("전체 항목 수"),
                                fieldWithPath("data.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                fieldWithPath("data.isFirst").type(JsonFieldType.BOOLEAN).description("첫 페이지 여부"),
                                fieldWithPath("data.isLast").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부")
                        )
                ));
    }
}
