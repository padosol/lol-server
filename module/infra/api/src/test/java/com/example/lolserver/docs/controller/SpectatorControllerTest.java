package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.spectator.SpectatorController;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.domain.queue_type.application.QueueTypeService;
import com.example.lolserver.domain.queue_type.domain.QueueInfo;
import com.example.lolserver.domain.spectator.application.SpectatorService;
import com.example.lolserver.domain.spectator.application.model.BannedChampionReadModel;
import com.example.lolserver.domain.spectator.application.model.CurrentGameInfoReadModel;
import com.example.lolserver.domain.spectator.application.model.ParticipantReadModel;
import com.example.lolserver.domain.spectator.application.model.PerksReadModel;
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

@DisplayName("SpectatorController 테스트")
@ExtendWith(MockitoExtension.class)
class SpectatorControllerTest extends RestDocsSupport {

    @Mock
    private QueueTypeService queueTypeService;

    @Mock
    private SpectatorService spectatorService;

    @Override
    protected Object initController() {
        return new SpectatorController(queueTypeService, spectatorService);
    }

    @DisplayName("큐 탭 정보 조회 API")
    @Test
    void findAllQueueInfoForTab_큐탭정보조회_성공() throws Exception {
        // given
        List<QueueInfo> queueInfos = List.of(
                new QueueInfo(420L, "솔로 랭크", true),
                new QueueInfo(430L, "일반", true),
                new QueueInfo(450L, "칼바람 나락", true)
        );

        given(queueTypeService.findAllByIsTabTrue()).willReturn(queueInfos);

        // when & then
        mockMvc.perform(
                        get("/api/v1/queue-tab")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("spectator-queue-tab",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY)
                                        .description("큐 정보 목록"),
                                fieldWithPath("data[].queueId").type(JsonFieldType.NUMBER)
                                        .description("큐 ID"),
                                fieldWithPath("data[].queueName").type(JsonFieldType.STRING)
                                        .description("큐 이름")
                        )
                ));
    }

    @DisplayName("현재 진행 중인 게임 정보 조회 - 게임 중인 경우")
    @Test
    void getCurrentGameInfo_게임중인경우_성공() throws Exception {
        // given
        String region = "kr";
        String puuid = "test-puuid-12345";

        PerksReadModel perks = new PerksReadModel(8100L, 8300L, List.of(8112L, 8126L, 8138L, 8135L, 8233L, 8237L));

        List<ParticipantReadModel> participants = List.of(
                new ParticipantReadModel("Player1#KR1", "puuid1", 1L, 100L, 4L, 14L, 1L, false, perks),
                new ParticipantReadModel("Player2#KR1", "puuid2", 2L, 200L, 7L, 11L, 2L, false, perks)
        );

        List<BannedChampionReadModel> bannedChampions = List.of(
                new BannedChampionReadModel(157L, 100L, 1),
                new BannedChampionReadModel(238L, 200L, 2)
        );

        CurrentGameInfoReadModel gameInfo = new CurrentGameInfoReadModel(
                12345L,
                "MATCHED_GAME",
                "CLASSIC",
                11L,
                420L,
                1706400000000L,
                600L,
                "KR",
                "encryptionKey123",
                participants,
                bannedChampions
        );

        given(spectatorService.getCurrentGameInfo(puuid, region)).willReturn(gameInfo);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{region}/spectator/active-games/by-puuid/{puuid}", region, puuid)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("spectator-active-game",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("지역 (kr, na, euw 등)"),
                                parameterWithName("puuid").description("소환사 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.gameId").type(JsonFieldType.NUMBER)
                                        .description("게임 ID"),
                                fieldWithPath("data.gameType").type(JsonFieldType.STRING)
                                        .description("게임 타입 (e.g., MATCHED_GAME)"),
                                fieldWithPath("data.gameMode").type(JsonFieldType.STRING)
                                        .description("게임 모드 (e.g., CLASSIC)"),
                                fieldWithPath("data.mapId").type(JsonFieldType.NUMBER)
                                        .description("맵 ID"),
                                fieldWithPath("data.gameQueueConfigId").type(JsonFieldType.NUMBER)
                                        .description("게임 큐 설정 ID (e.g., 420: 솔로 랭크, 430: 일반)"),
                                fieldWithPath("data.gameStartTime").type(JsonFieldType.NUMBER)
                                        .description("게임 시작 시간 (Unix Timestamp)"),
                                fieldWithPath("data.gameLength").type(JsonFieldType.NUMBER)
                                        .description("게임 진행 시간 (초)"),
                                fieldWithPath("data.platformId").type(JsonFieldType.STRING)
                                        .description("플랫폼 ID (e.g., KR)"),
                                fieldWithPath("data.encryptionKey").type(JsonFieldType.STRING)
                                        .description("관전 암호화 키"),
                                fieldWithPath("data.participants[]").type(JsonFieldType.ARRAY)
                                        .description("참여자 목록"),
                                fieldWithPath("data.participants[].riotId").type(JsonFieldType.STRING)
                                        .description("Riot ID (gameName#tagLine)"),
                                fieldWithPath("data.participants[].puuid").type(JsonFieldType.STRING)
                                        .description("소환사 PUUID"),
                                fieldWithPath("data.participants[].championId").type(JsonFieldType.NUMBER)
                                        .description("챔피언 ID"),
                                fieldWithPath("data.participants[].teamId").type(JsonFieldType.NUMBER)
                                        .description("팀 ID (100: 블루팀, 200: 레드팀)"),
                                fieldWithPath("data.participants[].spell1Id").type(JsonFieldType.NUMBER)
                                        .description("소환사 주문 1 ID"),
                                fieldWithPath("data.participants[].spell2Id").type(JsonFieldType.NUMBER)
                                        .description("소환사 주문 2 ID"),
                                fieldWithPath("data.participants[].profileIconId").type(JsonFieldType.NUMBER)
                                        .description("프로필 아이콘 ID"),
                                fieldWithPath("data.participants[].isBot").type(JsonFieldType.BOOLEAN)
                                        .description("봇 여부"),
                                fieldWithPath("data.participants[].perks").type(JsonFieldType.OBJECT)
                                        .description("룬 정보"),
                                fieldWithPath("data.participants[].perks.perkStyle").type(JsonFieldType.NUMBER)
                                        .description("핵심 룬 스타일 ID"),
                                fieldWithPath("data.participants[].perks.perkSubStyle").type(JsonFieldType.NUMBER)
                                        .description("보조 룬 스타일 ID"),
                                fieldWithPath("data.participants[].perks.perkIds[]").type(JsonFieldType.ARRAY)
                                        .description("선택한 모든 룬 ID 목록"),
                                fieldWithPath("data.bannedChampions[]").type(JsonFieldType.ARRAY)
                                        .description("밴 된 챔피언 목록"),
                                fieldWithPath("data.bannedChampions[].championId").type(JsonFieldType.NUMBER)
                                        .description("밴 된 챔피언 ID"),
                                fieldWithPath("data.bannedChampions[].teamId").type(JsonFieldType.NUMBER)
                                        .description("밴 한 팀 ID"),
                                fieldWithPath("data.bannedChampions[].pickTurn").type(JsonFieldType.NUMBER)
                                        .description("밴 순서")
                        )
                ));
    }

    @DisplayName("현재 진행 중인 게임 정보 조회 - 게임 중이 아닌 경우")
    @Test
    void getCurrentGameInfo_게임중아닌경우_성공() throws Exception {
        // given
        String region = "kr";
        String puuid = "test-puuid-12345";

        given(spectatorService.getCurrentGameInfo(puuid, region)).willReturn(null);

        // when & then
        mockMvc.perform(
                        get("/api/v1/{region}/spectator/active-games/by-puuid/{puuid}", region, puuid)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("spectator-active-game-not-found",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("region").description("지역 (kr, na, euw 등)"),
                                parameterWithName("puuid").description("소환사 PUUID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, FAIL)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data").type(JsonFieldType.NULL)
                                        .description("게임 중이 아닌 경우 null")
                        )
                ));
    }
}
