package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.championstats.ChampionStatsController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.championstats.application.ChampionStatsService;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionPositionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
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
        String platformId = "kr";
        int championId = 266;
        String patch = "14.24";

        ChampionMatchupReadModel matchup = new ChampionMatchupReadModel(86, 200, 110, 55.0);
        ChampionItemBuildReadModel itemBuild = new ChampionItemBuildReadModel("3078,3053,3065", 300, 170, 56.67);
        ChampionRuneBuildReadModel runeBuild = new ChampionRuneBuildReadModel(
                8000, "8010,9111,9104,8299", 8400, "8446,8451", 250, 140, 56.0
        );
        ChampionSkillBuildReadModel skillBuild = new ChampionSkillBuildReadModel(
                "Q,E,W,Q,Q,R,Q,E,Q,E,R,E,E,W,W", 400, 220, 55.0
        );

        ChampionPositionStatsReadModel positionStats = new ChampionPositionStatsReadModel(
                "TOP", 55.0, 1500,
                List.of(matchup),
                List.of(itemBuild),
                List.of(runeBuild),
                List.of(skillBuild)
        );

        ChampionStatsReadModel response = new ChampionStatsReadModel(
                "EMERALD",
                List.of(positionStats)
        );

        given(championStatsService.getChampionStats(anyInt(), anyString(), anyString(), anyString()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{platformId}/champion-stats", platformId)
                                .param("championId", String.valueOf(championId))
                                .param("patch", patch)
                                .param("tier", "EMERALD")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("champion-stats-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("platformId").description("플랫폼 ID (e.g., kr)")
                        ),
                        queryParameters(
                                parameterWithName("championId").description("챔피언 ID (e.g., 266)"),
                                parameterWithName("patch").description("패치 버전 (e.g., 14.24)"),
                                parameterWithName("tier").description("티어 (e.g., EMERALD)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),

                                fieldWithPath("data.tier").type(JsonFieldType.STRING).description("티어 (e.g., EMERALD)"),

                                // stats
                                fieldWithPath("data.stats[]").type(JsonFieldType.ARRAY).description("포지션별 통계 목록"),
                                fieldWithPath("data.stats[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.stats[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.stats[].totalCount").type(JsonFieldType.NUMBER).description("총 게임 수"),

                                // matchups
                                fieldWithPath("data.stats[].matchups[]").type(JsonFieldType.ARRAY).description("상대 챔피언별 매치업 목록"),
                                fieldWithPath("data.stats[].matchups[].opponentChampionId").type(JsonFieldType.NUMBER).description("상대 챔피언 ID"),
                                fieldWithPath("data.stats[].matchups[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.stats[].matchups[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.stats[].matchups[].totalWinRate").type(JsonFieldType.NUMBER).description("승률"),

                                // itemBuilds
                                fieldWithPath("data.stats[].itemBuilds[]").type(JsonFieldType.ARRAY).description("아이템 빌드 목록"),
                                fieldWithPath("data.stats[].itemBuilds[].itemsSorted").type(JsonFieldType.STRING).description("아이템 빌드 (정렬된 ID)"),
                                fieldWithPath("data.stats[].itemBuilds[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.stats[].itemBuilds[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.stats[].itemBuilds[].totalWinRate").type(JsonFieldType.NUMBER).description("승률"),

                                // runeBuilds
                                fieldWithPath("data.stats[].runeBuilds[]").type(JsonFieldType.ARRAY).description("룬 빌드 목록"),
                                fieldWithPath("data.stats[].runeBuilds[].primaryStyleId").type(JsonFieldType.NUMBER).description("주 룬 스타일 ID"),
                                fieldWithPath("data.stats[].runeBuilds[].primaryPerkIds").type(JsonFieldType.STRING).description("주 룬 ID 목록"),
                                fieldWithPath("data.stats[].runeBuilds[].subStyleId").type(JsonFieldType.NUMBER).description("보조 룬 스타일 ID"),
                                fieldWithPath("data.stats[].runeBuilds[].subPerkIds").type(JsonFieldType.STRING).description("보조 룬 ID 목록"),
                                fieldWithPath("data.stats[].runeBuilds[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.stats[].runeBuilds[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.stats[].runeBuilds[].totalWinRate").type(JsonFieldType.NUMBER).description("승률"),

                                // skillBuilds
                                fieldWithPath("data.stats[].skillBuilds[]").type(JsonFieldType.ARRAY).description("스킬 빌드 목록"),
                                fieldWithPath("data.stats[].skillBuilds[].skillOrder15").type(JsonFieldType.STRING).description("15레벨까지 스킬 순서"),
                                fieldWithPath("data.stats[].skillBuilds[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),
                                fieldWithPath("data.stats[].skillBuilds[].totalWins").type(JsonFieldType.NUMBER).description("총 승리 수"),
                                fieldWithPath("data.stats[].skillBuilds[].totalWinRate").type(JsonFieldType.NUMBER).description("승률")
                        )
                ));
    }
}
