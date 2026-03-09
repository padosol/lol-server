package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.championstats.ChampionStatsController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.championstats.application.ChampionStatsService;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionPositionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;
import java.util.Map;

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

        ChampionMatchupReadModel matchup = new ChampionMatchupReadModel(86, 200, 0.55, 0.4);
        ChampionRuneBuildReadModel runeBuild = new ChampionRuneBuildReadModel(
                8000, 8400, 8010, 9111, 9104, 8299, 8446, 8451, 5002, 5008, 5005, 250, 0.56, 0.5
        );
        ChampionSpellStatsReadModel spellStats = new ChampionSpellStatsReadModel(4, 14, 800, 0.52, 0.8);
        ChampionSkillBuildReadModel skillBuild = new ChampionSkillBuildReadModel(
                "Q,E,W,Q,Q,R,Q,E,Q,E,R,E,E,W,W", 400, 0.55, 0.4
        );
        ChampionStartItemBuildReadModel startItemBuild = new ChampionStartItemBuildReadModel(
                "1054,2003", 600, 0.51, 0.6
        );
        ChampionItemBuildReadModel itemBuild = new ChampionItemBuildReadModel("3078,3053,3065", 300, 0.5667, 0.3);
        ChampionItemStatsReadModel itemStats = new ChampionItemStatsReadModel(3078, "Trinity Force", 500, 0.55, 0.5);

        ChampionPositionStatsReadModel positionStats = new ChampionPositionStatsReadModel(
                "TOP", 0.55, 1500,
                List.of(matchup),
                List.of(runeBuild),
                List.of(spellStats),
                List.of(skillBuild),
                List.of(startItemBuild),
                List.of(itemBuild),
                Map.of(1, List.of(itemStats))
        );

        ChampionStatsReadModel response = new ChampionStatsReadModel("EMERALD", List.of(positionStats));

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

                                // positions (리스트)
                                fieldWithPath("data.positions[]").type(JsonFieldType.ARRAY).description("포지션별 통계 목록"),
                                fieldWithPath("data.positions[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.positions[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),

                                // matchups
                                fieldWithPath("data.positions[].matchups[]").type(JsonFieldType.ARRAY).description("상대 챔피언별 매치업 목록"),
                                fieldWithPath("data.positions[].matchups[].opponentChampionId").type(JsonFieldType.NUMBER).description("상대 챔피언 ID"),
                                fieldWithPath("data.positions[].matchups[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].matchups[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].matchups[].pickRate").type(JsonFieldType.NUMBER).description("대면 비율"),

                                // runeBuilds
                                fieldWithPath("data.positions[].runeBuilds[]").type(JsonFieldType.ARRAY).description("룬 빌드 목록"),
                                fieldWithPath("data.positions[].runeBuilds[].primaryStyleId").type(JsonFieldType.NUMBER).description("주 룬 스타일 ID"),
                                fieldWithPath("data.positions[].runeBuilds[].subStyleId").type(JsonFieldType.NUMBER).description("보조 룬 스타일 ID"),
                                fieldWithPath("data.positions[].runeBuilds[].primaryPerk0").type(JsonFieldType.NUMBER).description("주 룬 키스톤"),
                                fieldWithPath("data.positions[].runeBuilds[].primaryPerk1").type(JsonFieldType.NUMBER).description("주 룬 슬롯 1"),
                                fieldWithPath("data.positions[].runeBuilds[].primaryPerk2").type(JsonFieldType.NUMBER).description("주 룬 슬롯 2"),
                                fieldWithPath("data.positions[].runeBuilds[].primaryPerk3").type(JsonFieldType.NUMBER).description("주 룬 슬롯 3"),
                                fieldWithPath("data.positions[].runeBuilds[].subPerk0").type(JsonFieldType.NUMBER).description("보조 룬 슬롯 0"),
                                fieldWithPath("data.positions[].runeBuilds[].subPerk1").type(JsonFieldType.NUMBER).description("보조 룬 슬롯 1"),
                                fieldWithPath("data.positions[].runeBuilds[].statPerkDefense").type(JsonFieldType.NUMBER).description("방어 스탯 룬"),
                                fieldWithPath("data.positions[].runeBuilds[].statPerkFlex").type(JsonFieldType.NUMBER).description("유연 스탯 룬"),
                                fieldWithPath("data.positions[].runeBuilds[].statPerkOffense").type(JsonFieldType.NUMBER).description("공격 스탯 룬"),
                                fieldWithPath("data.positions[].runeBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].runeBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].runeBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),

                                // spellStats
                                fieldWithPath("data.positions[].spellStats[]").type(JsonFieldType.ARRAY).description("소환사 주문 조합 목록"),
                                fieldWithPath("data.positions[].spellStats[].summoner1Id").type(JsonFieldType.NUMBER).description("소환사 주문 1 ID"),
                                fieldWithPath("data.positions[].spellStats[].summoner2Id").type(JsonFieldType.NUMBER).description("소환사 주문 2 ID"),
                                fieldWithPath("data.positions[].spellStats[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].spellStats[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].spellStats[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),

                                // skillBuilds
                                fieldWithPath("data.positions[].skillBuilds[]").type(JsonFieldType.ARRAY).description("스킬 빌드 목록"),
                                fieldWithPath("data.positions[].skillBuilds[].skillBuild").type(JsonFieldType.STRING).description("스킬 빌드 순서"),
                                fieldWithPath("data.positions[].skillBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].skillBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].skillBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),

                                // startItemBuilds
                                fieldWithPath("data.positions[].startItemBuilds[]").type(JsonFieldType.ARRAY).description("시작 아이템 빌드 목록"),
                                fieldWithPath("data.positions[].startItemBuilds[].startItems").type(JsonFieldType.STRING).description("시작 아이템 ID 목록"),
                                fieldWithPath("data.positions[].startItemBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].startItemBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].startItemBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),

                                // itemBuilds (3코어)
                                fieldWithPath("data.positions[].itemBuilds[]").type(JsonFieldType.ARRAY).description("3코어 아이템 빌드 목록"),
                                fieldWithPath("data.positions[].itemBuilds[].itemBuild").type(JsonFieldType.STRING).description("아이템 빌드 순서"),
                                fieldWithPath("data.positions[].itemBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].itemBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].itemBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),

                                // itemStatsByOrder
                                fieldWithPath("data.positions[].itemStatsByOrder").type(JsonFieldType.OBJECT).description("코어 순서별 완성 아이템 통계 (키: 1, 2, 3)"),
                                fieldWithPath("data.positions[].itemStatsByOrder.1[]").type(JsonFieldType.ARRAY).description("1코어 아이템 통계"),
                                fieldWithPath("data.positions[].itemStatsByOrder.1[].itemId").type(JsonFieldType.NUMBER).description("아이템 ID"),
                                fieldWithPath("data.positions[].itemStatsByOrder.1[].itemName").type(JsonFieldType.STRING).description("아이템 이름"),
                                fieldWithPath("data.positions[].itemStatsByOrder.1[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].itemStatsByOrder.1[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].itemStatsByOrder.1[].pickRate").type(JsonFieldType.NUMBER).description("픽률")
                        )
                ));
    }

    @DisplayName("포지션별 챔피언 승률/픽률/밴률 조회 API")
    @Test
    void getChampionStatsByPosition() throws Exception {
        // given
        String platformId = "kr";
        List<PositionChampionStatsReadModel> response = List.of(
                new PositionChampionStatsReadModel("TOP", List.of(
                        new ChampionRateReadModel(266, 0.5200, 0.0800, 0.0500, 1500, "OP"),
                        new ChampionRateReadModel(122, 0.4800, 0.0600, 0.0300, 1200, "3")
                )),
                new PositionChampionStatsReadModel("JUNGLE", List.of(
                        new ChampionRateReadModel(64, 0.5100, 0.1000, 0.0700, 2000, "1")
                ))
        );
        given(championStatsService.getChampionStatsByPosition(anyString(), anyString(), anyString()))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{platformId}/champion-stats/positions", platformId)
                                .param("patch", "16.1")
                                .param("tier", "EMERALD")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("champion-stats-positions",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("platformId").description("플랫폼 ID (e.g., kr)")
                        ),
                        queryParameters(
                                parameterWithName("patch").description("패치 버전 (e.g., 16.1)"),
                                parameterWithName("tier").description("티어 (e.g., EMERALD)")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("포지션별 챔피언 통계 목록"),
                                fieldWithPath("data[].teamPosition").type(JsonFieldType.STRING)
                                        .description("포지션 (TOP, JUNGLE, MIDDLE, BOTTOM, UTILITY)"),
                                fieldWithPath("data[].champions[]").type(JsonFieldType.ARRAY)
                                        .description("해당 포지션의 챔피언 목록"),
                                fieldWithPath("data[].champions[].championId").type(JsonFieldType.NUMBER)
                                        .description("챔피언 ID"),
                                fieldWithPath("data[].champions[].winRate").type(JsonFieldType.NUMBER)
                                        .description("승률"),
                                fieldWithPath("data[].champions[].pickRate").type(JsonFieldType.NUMBER)
                                        .description("픽률"),
                                fieldWithPath("data[].champions[].banRate").type(JsonFieldType.NUMBER)
                                        .description("밴률"),
                                fieldWithPath("data[].champions[].tier").type(JsonFieldType.STRING)
                                        .description("METAPICK 티어 (OP, 1, 2, 3, 4, 5)")
                        )
                ));
    }
}
