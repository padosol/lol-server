package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.rank.RankController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.rank.application.RankService;
import com.example.lolserver.domain.rank.application.dto.RankResponse;
import com.example.lolserver.domain.rank.application.dto.RankSearchDto;
import com.example.lolserver.domain.rank.domain.Rank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
        String region = "kr";

        RankSearchDto searchDto = new RankSearchDto();
        searchDto.setRegion(region);
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
                .leaguePoints(1234)
                .champions(List.of("Ahri", "Zed"))
                .build();

        List<RankResponse> response = List.of(new RankResponse(rank));

        given(rankService.getRanks(any(RankSearchDto.class))).willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{region}/rank", region)
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
                                parameterWithName("region").description("조회할 지역 (e.g., kr)")
                        ),
                        queryParameters(
                                parameterWithName("rankType").description("게임 타입 (SOLO, FLEX)").optional(),
                                parameterWithName("page").description("페이지 번호 (1부터 시작)").optional(),
                                parameterWithName("tier").description("조회할 티어 (e.g., CHALLENGER, GOLD)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[].puuid").type(JsonFieldType.STRING).description("소환사 PUUID"),
                                fieldWithPath("data[].currentRank").type(JsonFieldType.NUMBER).description("현재 순위"),
                                fieldWithPath("data[].rankChange").type(JsonFieldType.NUMBER).description("순위 변동 (양수: 상승, 음수: 하락)"),
                                fieldWithPath("data[].gameName").type(JsonFieldType.STRING).description("소환사 게임 이름"),
                                fieldWithPath("data[].tagLine").type(JsonFieldType.STRING).description("태그 라인"),
                                fieldWithPath("data[].wins").type(JsonFieldType.NUMBER).description("승리 수"),
                                fieldWithPath("data[].losses").type(JsonFieldType.NUMBER).description("패배 수"),
                                fieldWithPath("data[].winRate").type(JsonFieldType.NUMBER).description("승률 (%)"),
                                fieldWithPath("data[].tier").type(JsonFieldType.STRING).description("티어"),
                                fieldWithPath("data[].leaguePoints").type(JsonFieldType.NUMBER).description("리그 포인트(LP)"),
                                fieldWithPath("data[].champions").type(JsonFieldType.ARRAY).description("주요 챔피언 이름 목록")
                        )
                ));
    }
}
