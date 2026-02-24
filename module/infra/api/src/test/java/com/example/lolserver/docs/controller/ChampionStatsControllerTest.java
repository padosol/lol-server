package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.championstats.ChampionStatsController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.championstats.application.ChampionStatsService;
import com.example.lolserver.domain.championstats.application.dto.ChampionItemBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionMatchupResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionRuneBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionSkillBuildResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionStatsResponse;
import com.example.lolserver.domain.championstats.application.dto.ChampionWinRateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChampionStatsControllerTest extends RestDocsSupport {

    @Mock
    private ChampionStatsService championStatsService;

    @InjectMocks
    private ChampionStatsController championStatsController;

    @Override
    protected Object initController() {
        return championStatsController;
    }

    @DisplayName("챔피언 통계 조회 API")
    @Test
    void getChampionStats() throws Exception {
        // given
        String region = "kr";
        int championId = 266;
        String patch = "14.24";
        String platformId = "KR";

        ChampionWinRateResponse winRate = new ChampionWinRateResponse(
                266, "TOP", 1500, 825, 55.0
        );

        ChampionMatchupResponse matchup = new ChampionMatchupResponse(
                266, 86, "TOP", 200, 110, 55.0
        );

        ChampionItemBuildResponse itemBuild = new ChampionItemBuildResponse(
                266, "TOP", "3078,3053,3065", 300, 170, 56.67
        );

        ChampionRuneBuildResponse runeBuild = new ChampionRuneBuildResponse(
                266, "TOP", 8000, "8010,9111,9104,8299", 8400, "8446,8451",
                250, 140, 56.0
        );

        ChampionSkillBuildResponse skillBuild = new ChampionSkillBuildResponse(
                266, "TOP", "Q,E,W,Q,Q,R,Q,E,Q,E,R,E,E,W,W", 400, 220, 55.0
        );

        ChampionStatsResponse response = new ChampionStatsResponse(
                List.of(winRate),
                List.of(matchup),
                List.of(itemBuild),
                List.of(runeBuild),
                List.of(skillBuild)
        );

        given(championStatsService.getChampionStats(anyInt(), anyString(), anyString()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{region}/champion-stats", region)
                                .param("championId", String.valueOf(championId))
                                .param("patch", patch)
                                .param("platformId", platformId)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("champion-stats-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("조회할 지역 (e.g., kr)")
                        ),
                        queryParameters(
                                parameterWithName("championId").description("챔피언 ID (e.g., 266)"),
                                parameterWithName("patch").description("패치 버전 (e.g., 14.24)"),
                                parameterWithName("platformId").description("플랫폼 ID (e.g., KR)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),

                                // winRates
                                fieldWithPath("data.winRates[]").type(JsonFieldType.ARRAY).description("포지션별 승률 목록"),
                                fieldWithPath("data.winRates[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data.winRates[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.winRates[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.winRates[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.winRates[].totalWinRate").type(JsonFieldType.NUMBER).description("승률"),

                                // matchups
                                fieldWithPath("data.matchups[]").type(JsonFieldType.ARRAY).description("상대 챔피언별 매치업 목록"),
                                fieldWithPath("data.matchups[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data.matchups[].opponentChampionId").type(JsonFieldType.NUMBER).description("상대 챔피언 ID"),
                                fieldWithPath("data.matchups[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.matchups[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.matchups[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.matchups[].totalWinRate").type(JsonFieldType.NUMBER).description("승률"),

                                // itemBuilds
                                fieldWithPath("data.itemBuilds[]").type(JsonFieldType.ARRAY).description("아이템 빌드 목록"),
                                fieldWithPath("data.itemBuilds[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data.itemBuilds[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.itemBuilds[].itemsSorted").type(JsonFieldType.STRING).description("아이템 빌드 (정렬된 ID)"),
                                fieldWithPath("data.itemBuilds[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.itemBuilds[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.itemBuilds[].totalWinRate").type(JsonFieldType.NUMBER).description("승률"),

                                // runeBuilds
                                fieldWithPath("data.runeBuilds[]").type(JsonFieldType.ARRAY).description("룬 빌드 목록"),
                                fieldWithPath("data.runeBuilds[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data.runeBuilds[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.runeBuilds[].primaryStyleId").type(JsonFieldType.NUMBER).description("주 룬 스타일 ID"),
                                fieldWithPath("data.runeBuilds[].primaryPerkIds").type(JsonFieldType.STRING).description("주 룬 ID 목록"),
                                fieldWithPath("data.runeBuilds[].subStyleId").type(JsonFieldType.NUMBER).description("보조 룬 스타일 ID"),
                                fieldWithPath("data.runeBuilds[].subPerkIds").type(JsonFieldType.STRING).description("보조 룬 ID 목록"),
                                fieldWithPath("data.runeBuilds[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.runeBuilds[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.runeBuilds[].totalWinRate").type(JsonFieldType.NUMBER).description("승률"),

                                // skillBuilds
                                fieldWithPath("data.skillBuilds[]").type(JsonFieldType.ARRAY).description("스킬 빌드 목록"),
                                fieldWithPath("data.skillBuilds[].championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data.skillBuilds[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.skillBuilds[].skillOrder15").type(JsonFieldType.STRING).description("15레벨까지 스킬 순서"),
                                fieldWithPath("data.skillBuilds[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.skillBuilds[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.skillBuilds[].totalWinRate").type(JsonFieldType.NUMBER).description("승률")
                        )
                ));
    }
}
