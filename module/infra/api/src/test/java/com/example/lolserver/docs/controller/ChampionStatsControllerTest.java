package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.championstats.ChampionStatsController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.championstats.application.port.in.ChampionStatsQueryUseCase;
import com.example.lolserver.domain.championstats.application.model.ChampionAverageStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionBootBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionMatchupReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionPositionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRuneBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSkillBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionSpellStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStartItemBuildReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionRateReadModel;
import com.example.lolserver.domain.championstats.application.model.ChampionTimelineReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionChampionStatsReadModel;
import com.example.lolserver.domain.championstats.application.model.PositionTimelineReadModel;
import com.example.lolserver.domain.championstats.application.model.TimelineFrameReadModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import com.example.lolserver.TierFilter;

import static org.mockito.ArgumentMatchers.any;
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
    private ChampionStatsQueryUseCase championStatsService;

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

        ChampionMatchupReadModel topMatchup = ChampionMatchupReadModel.top(86, 200, 0.55, 0.4);
        ChampionMatchupReadModel bottomMatchup = ChampionMatchupReadModel.bottom(122, 180, 0.45, 0.36);
        ChampionRuneBuildReadModel runeBuild = new ChampionRuneBuildReadModel(
                8000, 8400, 8010, 9111, 9104, 8299, 8446, 8451, 250, 0.56, 0.5
        );
        ChampionSpellStatsReadModel spellStats = new ChampionSpellStatsReadModel(4, 14, 800, 0.52, 0.8);
        ChampionSkillBuildReadModel skillBuild = new ChampionSkillBuildReadModel(
                "Q,E,W,Q,Q,R,Q,E,Q,E,R,E,E,W,W", 400, 0.55, 0.4
        );
        ChampionStartItemBuildReadModel startItemBuild = new ChampionStartItemBuildReadModel(
                List.of(1054, 2003), 600, 0.51, 0.6
        );
        ChampionBootBuildReadModel bootBuild = new ChampionBootBuildReadModel(3047, 700, 0.53, 0.7);
        ChampionItemBuildReadModel itemBuild = new ChampionItemBuildReadModel(
                List.of(3078, 3053, 3065), 300, 0.5667, 0.3
        );

        ChampionAverageStatsReadModel averages = new ChampionAverageStatsReadModel(
                "TOP", 6.2, 5.4, 7.8, 2.59, 380.5, 65.3, 12.1
        );

        ChampionPositionStatsReadModel positionStats = new ChampionPositionStatsReadModel(
                "TOP", 0.55, 0.075, 0.04, "S", 1500,
                averages,
                List.of(topMatchup, bottomMatchup),
                List.of(runeBuild),
                List.of(spellStats),
                List.of(skillBuild),
                List.of(startItemBuild),
                List.of(bootBuild),
                List.of(itemBuild)
        );

        ChampionStatsReadModel response = new ChampionStatsReadModel("EMERALD", List.of(positionStats));

        given(championStatsService.getChampionStats(anyInt(), anyString(), anyString(), any(TierFilter.class)))
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
                                parameterWithName("tier").description("티어 필터. 단일 티어(e.g., EMERALD) 또는 범위 티어(e.g., MASTER+)를 지원합니다.")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),

                                fieldWithPath("data.tier").type(JsonFieldType.STRING).description("티어 (e.g., EMERALD)"),

                                // positions (리스트)
                                fieldWithPath("data.positions[]").type(JsonFieldType.ARRAY).description("포지션별 통계 목록"),
                                fieldWithPath("data.positions[].teamPosition").type(JsonFieldType.STRING).description("포지션"),
                                fieldWithPath("data.positions[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].pickRate").type(JsonFieldType.NUMBER).description("픽률 (해당 라인 기준)"),
                                fieldWithPath("data.positions[].banRate").type(JsonFieldType.NUMBER).description("밴률 (라인 무관)"),
                                fieldWithPath("data.positions[].tier").type(JsonFieldType.STRING).description("티어 등급 (S+, S, A, B, C, D — /champion-stats/positions 와 동일 룰)"),
                                fieldWithPath("data.positions[].totalGames").type(JsonFieldType.NUMBER).description("총 게임 수"),

                                // averages (챔피언 단위 평균 통계)
                                fieldWithPath("data.positions[].averages").type(JsonFieldType.OBJECT).description("챔피언 단위 평균 통계 (BQ 미적재 시 null)").optional(),
                                fieldWithPath("data.positions[].averages.teamPosition").type(JsonFieldType.STRING).description("포지션").optional(),
                                fieldWithPath("data.positions[].averages.avgKills").type(JsonFieldType.NUMBER).description("평균 킬").optional(),
                                fieldWithPath("data.positions[].averages.avgDeaths").type(JsonFieldType.NUMBER).description("평균 데스").optional(),
                                fieldWithPath("data.positions[].averages.avgAssists").type(JsonFieldType.NUMBER).description("평균 어시스트").optional(),
                                fieldWithPath("data.positions[].averages.kda").type(JsonFieldType.NUMBER).description("KDA = (킬+어시) / max(데스, 1)").optional(),
                                fieldWithPath("data.positions[].averages.avgGoldPerMinute").type(JsonFieldType.NUMBER).description("분당 평균 골드 (gold_per_minute)").optional(),
                                fieldWithPath("data.positions[].averages.avgLaneCs10m").type(JsonFieldType.NUMBER).description("10분 시점 평균 라인 CS").optional(),
                                fieldWithPath("data.positions[].averages.avgJungleCs10m").type(JsonFieldType.NUMBER).description("10분 시점 평균 정글 CS").optional(),

                                // matchups (rankType=TOP: 잘 잡는 상대 / BOTTOM: 카운터)
                                fieldWithPath("data.positions[].matchups[]").type(JsonFieldType.ARRAY).description("매치업 목록 (rankType=TOP: 잘 잡는 상대, BOTTOM: 카운터)"),
                                fieldWithPath("data.positions[].matchups[].rankType").type(JsonFieldType.STRING).description("매치업 분류 (TOP=잘 잡는 상대 / BOTTOM=카운터)"),
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
                                fieldWithPath("data.positions[].runeBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].runeBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].runeBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),
                                fieldWithPath("data.positions[].runeBuilds[].sampleSize").type(JsonFieldType.NUMBER).description("이 빌드의 표본 크기 (= games)"),
                                fieldWithPath("data.positions[].runeBuilds[].totalSampleSize").type(JsonFieldType.NUMBER).description("챔프-라인 총 표본 크기"),
                                fieldWithPath("data.positions[].runeBuilds[].confidenceLowerBound").type(JsonFieldType.NUMBER).description("Wilson 신뢰구간(95%) 하한 — 추천 신뢰도 정렬 기준"),

                                // spellStats
                                fieldWithPath("data.positions[].spellStats[]").type(JsonFieldType.ARRAY).description("소환사 주문 조합 목록"),
                                fieldWithPath("data.positions[].spellStats[].summoner1Id").type(JsonFieldType.NUMBER).description("소환사 주문 1 ID"),
                                fieldWithPath("data.positions[].spellStats[].summoner2Id").type(JsonFieldType.NUMBER).description("소환사 주문 2 ID"),
                                fieldWithPath("data.positions[].spellStats[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].spellStats[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].spellStats[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),
                                fieldWithPath("data.positions[].spellStats[].sampleSize").type(JsonFieldType.NUMBER).description("이 조합의 표본 크기 (= games)"),
                                fieldWithPath("data.positions[].spellStats[].totalSampleSize").type(JsonFieldType.NUMBER).description("챔프-라인 총 표본 크기"),
                                fieldWithPath("data.positions[].spellStats[].confidenceLowerBound").type(JsonFieldType.NUMBER).description("Wilson 신뢰구간(95%) 하한"),

                                // skillBuilds
                                fieldWithPath("data.positions[].skillBuilds[]").type(JsonFieldType.ARRAY).description("스킬 빌드 목록"),
                                fieldWithPath("data.positions[].skillBuilds[].skillBuild").type(JsonFieldType.STRING).description("스킬 빌드 순서"),
                                fieldWithPath("data.positions[].skillBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].skillBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].skillBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),
                                fieldWithPath("data.positions[].skillBuilds[].sampleSize").type(JsonFieldType.NUMBER).description("이 빌드의 표본 크기 (= games)"),
                                fieldWithPath("data.positions[].skillBuilds[].totalSampleSize").type(JsonFieldType.NUMBER).description("챔프-라인 총 표본 크기"),
                                fieldWithPath("data.positions[].skillBuilds[].confidenceLowerBound").type(JsonFieldType.NUMBER).description("Wilson 신뢰구간(95%) 하한"),

                                // startItemBuilds
                                fieldWithPath("data.positions[].startItemBuilds[]").type(JsonFieldType.ARRAY).description("시작 아이템 빌드 목록"),
                                fieldWithPath("data.positions[].startItemBuilds[].startItems").type(JsonFieldType.ARRAY).description("시작 아이템 ID 배열 (number[])"),
                                fieldWithPath("data.positions[].startItemBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].startItemBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].startItemBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),
                                fieldWithPath("data.positions[].startItemBuilds[].sampleSize").type(JsonFieldType.NUMBER).description("이 빌드의 표본 크기 (= games)"),
                                fieldWithPath("data.positions[].startItemBuilds[].totalSampleSize").type(JsonFieldType.NUMBER).description("챔프-라인 총 표본 크기"),
                                fieldWithPath("data.positions[].startItemBuilds[].confidenceLowerBound").type(JsonFieldType.NUMBER).description("Wilson 신뢰구간(95%) 하한"),

                                // bootBuilds (신발)
                                fieldWithPath("data.positions[].bootBuilds[]").type(JsonFieldType.ARRAY).description("신발 빌드 목록"),
                                fieldWithPath("data.positions[].bootBuilds[].bootId").type(JsonFieldType.NUMBER).description("신발 아이템 ID"),
                                fieldWithPath("data.positions[].bootBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].bootBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].bootBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),
                                fieldWithPath("data.positions[].bootBuilds[].sampleSize").type(JsonFieldType.NUMBER).description("이 빌드의 표본 크기 (= games)"),
                                fieldWithPath("data.positions[].bootBuilds[].totalSampleSize").type(JsonFieldType.NUMBER).description("챔프-라인 총 표본 크기"),
                                fieldWithPath("data.positions[].bootBuilds[].confidenceLowerBound").type(JsonFieldType.NUMBER).description("Wilson 신뢰구간(95%) 하한"),

                                // itemBuilds (3코어)
                                fieldWithPath("data.positions[].itemBuilds[]").type(JsonFieldType.ARRAY).description("3코어 아이템 빌드 목록"),
                                fieldWithPath("data.positions[].itemBuilds[].itemBuild").type(JsonFieldType.ARRAY).description("아이템 ID 배열 (number[], 빌드 순서)"),
                                fieldWithPath("data.positions[].itemBuilds[].games").type(JsonFieldType.NUMBER).description("게임 수"),
                                fieldWithPath("data.positions[].itemBuilds[].winRate").type(JsonFieldType.NUMBER).description("승률"),
                                fieldWithPath("data.positions[].itemBuilds[].pickRate").type(JsonFieldType.NUMBER).description("픽률"),
                                fieldWithPath("data.positions[].itemBuilds[].sampleSize").type(JsonFieldType.NUMBER).description("이 빌드의 표본 크기 (= games)"),
                                fieldWithPath("data.positions[].itemBuilds[].totalSampleSize").type(JsonFieldType.NUMBER).description("챔프-라인 총 표본 크기"),
                                fieldWithPath("data.positions[].itemBuilds[].confidenceLowerBound").type(JsonFieldType.NUMBER).description("Wilson 신뢰구간(95%) 하한")
                        )
                ));
    }

    @DisplayName("챔피언 시간대별 평균 곡선 조회 API")
    @Test
    void getChampionTimeline() throws Exception {
        // given
        String platformId = "kr";
        ChampionTimelineReadModel response = new ChampionTimelineReadModel(
                266,
                "EMERALD",
                List.of(
                        new PositionTimelineReadModel("TOP", List.of(
                                new TimelineFrameReadModel(10, 4520.5, 78.2, 4810.0, 1500),
                                new TimelineFrameReadModel(15, 7210.4, 121.6, 7950.5, 1480),
                                new TimelineFrameReadModel(20, 10210.7, 168.9, 11460.0, 1310),
                                new TimelineFrameReadModel(25, 13150.0, 213.4, 15010.5, 980),
                                new TimelineFrameReadModel(30, 16290.2, 259.1, 18790.5, 540)
                        ))
                ));

        given(championStatsService.getChampionTimeline(anyInt(), anyString(), anyString(), any(TierFilter.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{platformId}/champion-stats/timeline", platformId)
                                .param("championId", "266")
                                .param("patch", "14.24")
                                .param("tier", "EMERALD")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andDo(document("champion-stats-timeline",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("platformId").description("플랫폼 ID (e.g., kr)")
                        ),
                        queryParameters(
                                parameterWithName("championId").description("챔피언 ID"),
                                parameterWithName("patch").description("패치 버전 (e.g., 14.24)"),
                                parameterWithName("tier").description("티어 필터. 단일 또는 범위 (e.g., MASTER+).")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING).description("API 응답 결과"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL).description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.championId").type(JsonFieldType.NUMBER).description("챔피언 ID"),
                                fieldWithPath("data.tier").type(JsonFieldType.STRING).description("티어 표시 (e.g., EMERALD, MASTER+)"),
                                fieldWithPath("data.positions[]").type(JsonFieldType.ARRAY).description("포지션별 시계열 목록"),
                                fieldWithPath("data.positions[].teamPosition").type(JsonFieldType.STRING).description("포지션 (TOP/JUNGLE/MIDDLE/BOTTOM/UTILITY)"),
                                fieldWithPath("data.positions[].frames[]").type(JsonFieldType.ARRAY).description("시간대별 프레임 (10/15/20/25/30분)"),
                                fieldWithPath("data.positions[].frames[].minute").type(JsonFieldType.NUMBER).description("분 단위 시각 (10/15/20/25/30)"),
                                fieldWithPath("data.positions[].frames[].avgGold").type(JsonFieldType.NUMBER).description("평균 누적 골드"),
                                fieldWithPath("data.positions[].frames[].avgCs").type(JsonFieldType.NUMBER).description("평균 CS (라인 + 정글 미니언)"),
                                fieldWithPath("data.positions[].frames[].avgXp").type(JsonFieldType.NUMBER).description("평균 누적 경험치"),
                                fieldWithPath("data.positions[].frames[].sampleSize").type(JsonFieldType.NUMBER).description("이 프레임에 도달한 게임 수")
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
        given(championStatsService.getChampionStatsByPosition(anyString(), anyString(), any(TierFilter.class)))
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
                                parameterWithName("tier").description("티어 필터. 단일 티어(e.g., EMERALD) 또는 범위 티어(e.g., MASTER+)를 지원합니다.")
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
                                        .description("티어 등급 (BigQuery 모드: S+, S, A, B, C, D / ClickHouse 모드: OP, 1, 2, 3, 4, 5)")
                        )
                ));
    }
}
