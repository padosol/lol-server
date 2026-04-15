package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.duo.DuoRequestController;
import com.example.lolserver.controller.duo.request.CreateDuoRequestRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.duo.application.model.DuoMatchResultReadModel;
import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.application.port.in.DuoRequestQueryUseCase;
import com.example.lolserver.domain.duo.application.port.in.DuoRequestUseCase;
import com.example.lolserver.support.SliceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.example.lolserver.domain.duo.domain.vo.MostChampion;
import com.example.lolserver.domain.duo.domain.vo.RecentGameSummary;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DuoRequestController 테스트")
@ExtendWith(MockitoExtension.class)
class DuoRequestControllerTest extends RestDocsSupport {

    @Mock
    private DuoRequestUseCase duoRequestUseCase;

    @Mock
    private DuoRequestQueryUseCase duoRequestQueryUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new DuoRequestController(duoRequestUseCase, duoRequestQueryUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new TestAuthenticatedMemberResolver()};
    }

    private DuoRequestReadModel sampleDuoRequest() {
        return DuoRequestReadModel.builder()
                .id(1L)
                .duoPostId(1L)
                .primaryLane("ADC")
                .desiredLane("SUPPORT")
                .hasMicrophone(false)
                .tier("SILVER")
                .rank("II")
                .leaguePoints(30)
                .memo("같이 하실 분")
                .status("PENDING")
                .mostChampions(sampleMostChampions())
                .recentGameSummary(sampleRecentGameSummary())
                .createdAt(LocalDateTime.of(2026, 4, 14, 11, 0, 0))
                .build();
    }

    private List<MostChampion> sampleMostChampions() {
        return List.of(
                new MostChampion(236, "루시안", 50, 30, 20),
                new MostChampion(103, "아리", 40, 25, 15)
        );
    }

    private RecentGameSummary sampleRecentGameSummary() {
        return new RecentGameSummary(7, 3, List.of(
                new RecentGameSummary.PlayedChampion(236, "루시안"),
                new RecentGameSummary.PlayedChampion(103, "아리")
        ));
    }

    private DuoMatchResultReadModel sampleAcceptedResult() {
        return DuoMatchResultReadModel.builder()
                .duoPostId(1L)
                .requestId(1L)
                .partnerGameName(null)
                .partnerTagLine(null)
                .status("ACCEPTED")
                .build();
    }

    private DuoMatchResultReadModel sampleConfirmedResult() {
        return DuoMatchResultReadModel.builder()
                .duoPostId(1L)
                .requestId(1L)
                .partnerGameName("Hide on bush")
                .partnerTagLine("KR1")
                .status("CONFIRMED")
                .build();
    }

    @DisplayName("듀오 요청 생성 API")
    @Test
    void createDuoRequest() throws Exception {
        given(duoRequestUseCase.createDuoRequest(eq(1L), eq(1L), any()))
                .willReturn(sampleDuoRequest());

        CreateDuoRequestRequest request = new CreateDuoRequestRequest(
                "ADC", "SUPPORT", false, "같이 하실 분");

        mockMvc.perform(
                        post("/api/duo/posts/{postId}/requests", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("duo-request-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("primaryLane").type(JsonFieldType.STRING)
                                        .description("주 라인 (TOP, JUNGLE, MID, ADC, SUPPORT)"),
                                fieldWithPath("desiredLane").type(JsonFieldType.STRING)
                                        .description("부 라인 (TOP, JUNGLE, MID, ADC, SUPPORT)"),
                                fieldWithPath("hasMicrophone").type(JsonFieldType.BOOLEAN)
                                        .description("마이크 보유 여부"),
                                fieldWithPath("memo").type(JsonFieldType.STRING)
                                        .description("메모 (최대 500자)")
                                        .optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("요청 ID"),
                                fieldWithPath("data.duoPostId").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.primaryLane").type(JsonFieldType.STRING)
                                        .description("주 라인"),
                                fieldWithPath("data.desiredLane").type(JsonFieldType.STRING)
                                        .description("부 라인"),
                                fieldWithPath("data.hasMicrophone").type(JsonFieldType.BOOLEAN)
                                        .description("마이크 보유 여부"),
                                fieldWithPath("data.tier").type(JsonFieldType.STRING)
                                        .description("티어")
                                        .optional(),
                                fieldWithPath("data.rank").type(JsonFieldType.STRING)
                                        .description("랭크")
                                        .optional(),
                                fieldWithPath("data.leaguePoints").type(JsonFieldType.NUMBER)
                                        .description("리그 포인트"),
                                fieldWithPath("data.memo").type(JsonFieldType.STRING)
                                        .description("메모")
                                        .optional(),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("요청 상태 (PENDING, ACCEPTED, CONFIRMED, REJECTED, CANCELLED)"),
                                fieldWithPath("data.mostChampions[].championId").type(JsonFieldType.NUMBER)
                                        .description("모스트 챔피언 ID"),
                                fieldWithPath("data.mostChampions[].championName").type(JsonFieldType.STRING)
                                        .description("모스트 챔피언 이름"),
                                fieldWithPath("data.mostChampions[].playCount").type(JsonFieldType.NUMBER)
                                        .description("플레이 횟수"),
                                fieldWithPath("data.mostChampions[].wins").type(JsonFieldType.NUMBER)
                                        .description("승리 횟수"),
                                fieldWithPath("data.mostChampions[].losses").type(JsonFieldType.NUMBER)
                                        .description("패배 횟수"),
                                fieldWithPath("data.recentGameSummary.wins").type(JsonFieldType.NUMBER)
                                        .description("최근 게임 승리 수"),
                                fieldWithPath("data.recentGameSummary.losses").type(JsonFieldType.NUMBER)
                                        .description("최근 게임 패배 수"),
                                fieldWithPath("data.recentGameSummary.playedChampions[].championId").type(JsonFieldType.NUMBER)
                                        .description("최근 플레이 챔피언 ID"),
                                fieldWithPath("data.recentGameSummary.playedChampions[].championName").type(JsonFieldType.STRING)
                                        .description("최근 플레이 챔피언 이름"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("요청 일시")
                        )
                ));
    }

    @DisplayName("게시글 요청 목록 조회 API")
    @Test
    void getDuoRequestsForPost() throws Exception {
        given(duoRequestQueryUseCase.getDuoRequestsForPost(eq(1L), eq(1L)))
                .willReturn(List.of(sampleDuoRequest()));

        mockMvc.perform(
                        get("/api/duo/posts/{postId}/requests", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-request-list-for-post",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data[].id").type(JsonFieldType.NUMBER)
                                        .description("요청 ID"),
                                fieldWithPath("data[].duoPostId").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data[].primaryLane").type(JsonFieldType.STRING)
                                        .description("요청자 주 라인"),
                                fieldWithPath("data[].desiredLane").type(JsonFieldType.STRING)
                                        .description("요청자 부 라인"),
                                fieldWithPath("data[].hasMicrophone").type(JsonFieldType.BOOLEAN)
                                        .description("요청자 마이크 보유 여부"),
                                fieldWithPath("data[].tier").type(JsonFieldType.STRING)
                                        .description("요청자 티어")
                                        .optional(),
                                fieldWithPath("data[].rank").type(JsonFieldType.STRING)
                                        .description("요청자 랭크")
                                        .optional(),
                                fieldWithPath("data[].leaguePoints").type(JsonFieldType.NUMBER)
                                        .description("요청자 리그 포인트"),
                                fieldWithPath("data[].memo").type(JsonFieldType.STRING)
                                        .description("요청자 메모")
                                        .optional(),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING)
                                        .description("요청 상태"),
                                fieldWithPath("data[].mostChampions[].championId").type(JsonFieldType.NUMBER)
                                        .description("요청자 모스트 챔피언 ID"),
                                fieldWithPath("data[].mostChampions[].championName").type(JsonFieldType.STRING)
                                        .description("요청자 모스트 챔피언 이름"),
                                fieldWithPath("data[].mostChampions[].playCount").type(JsonFieldType.NUMBER)
                                        .description("요청자 플레이 횟수"),
                                fieldWithPath("data[].mostChampions[].wins").type(JsonFieldType.NUMBER)
                                        .description("요청자 승리 횟수"),
                                fieldWithPath("data[].mostChampions[].losses").type(JsonFieldType.NUMBER)
                                        .description("요청자 패배 횟수"),
                                fieldWithPath("data[].recentGameSummary.wins").type(JsonFieldType.NUMBER)
                                        .description("요청자 최근 게임 승리 수"),
                                fieldWithPath("data[].recentGameSummary.losses").type(JsonFieldType.NUMBER)
                                        .description("요청자 최근 게임 패배 수"),
                                fieldWithPath("data[].recentGameSummary.playedChampions[].championId").type(JsonFieldType.NUMBER)
                                        .description("요청자 최근 플레이 챔피언 ID"),
                                fieldWithPath("data[].recentGameSummary.playedChampions[].championName").type(JsonFieldType.STRING)
                                        .description("요청자 최근 플레이 챔피언 이름"),
                                fieldWithPath("data[].createdAt").type(JsonFieldType.STRING)
                                        .description("요청 일시")
                        )
                ));
    }

    @DisplayName("듀오 요청 수락 API")
    @Test
    void acceptDuoRequest() throws Exception {
        given(duoRequestUseCase.acceptDuoRequest(eq(1L), eq(1L)))
                .willReturn(sampleAcceptedResult());

        mockMvc.perform(
                        put("/api/duo/requests/{requestId}/accept", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-request-accept",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("requestId").description("요청 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.duoPostId").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.requestId").type(JsonFieldType.NUMBER)
                                        .description("요청 ID"),
                                fieldWithPath("data.partnerGameName").type(JsonFieldType.NULL)
                                        .description("파트너 게임 이름 (수락 단계에서는 null)")
                                        .optional(),
                                fieldWithPath("data.partnerTagLine").type(JsonFieldType.NULL)
                                        .description("파트너 태그라인 (수락 단계에서는 null)")
                                        .optional(),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("요청 상태 (ACCEPTED)")
                        )
                ));
    }

    @DisplayName("듀오 요청 확정 API")
    @Test
    void confirmDuoRequest() throws Exception {
        given(duoRequestUseCase.confirmDuoRequest(eq(1L), eq(1L)))
                .willReturn(sampleConfirmedResult());

        mockMvc.perform(
                        put("/api/duo/requests/{requestId}/confirm", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-request-confirm",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("requestId").description("요청 ID")
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.duoPostId").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.requestId").type(JsonFieldType.NUMBER)
                                        .description("요청 ID"),
                                fieldWithPath("data.partnerGameName").type(JsonFieldType.STRING)
                                        .description("파트너 게임 이름"),
                                fieldWithPath("data.partnerTagLine").type(JsonFieldType.STRING)
                                        .description("파트너 태그라인"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("요청 상태 (CONFIRMED)")
                        )
                ));
    }

    @DisplayName("듀오 요청 거절 API")
    @Test
    void rejectDuoRequest() throws Exception {
        willDoNothing().given(duoRequestUseCase).rejectDuoRequest(eq(1L), eq(1L));

        mockMvc.perform(
                        put("/api/duo/requests/{requestId}/reject", 1L)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("duo-request-reject",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("requestId").description("요청 ID")
                        )
                ));
    }

    @DisplayName("듀오 요청 취소 API")
    @Test
    void cancelDuoRequest() throws Exception {
        willDoNothing().given(duoRequestUseCase).cancelDuoRequest(eq(1L), eq(1L));

        mockMvc.perform(
                        put("/api/duo/requests/{requestId}/cancel", 1L)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("duo-request-cancel",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("requestId").description("요청 ID")
                        )
                ));
    }

    @DisplayName("내 듀오 요청 목록 조회 API")
    @Test
    void getMyDuoRequests() throws Exception {
        SliceResult<DuoRequestReadModel> result = new SliceResult<>(
                List.of(sampleDuoRequest()),
                false);
        given(duoRequestQueryUseCase.getMyDuoRequests(eq(1L), eq(0)))
                .willReturn(result);

        mockMvc.perform(
                        get("/api/duo/me/requests")
                                .param("page", "0")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-request-my-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                subsectionWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                        .description("내 듀오 요청 목록"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN)
                                        .description("다음 페이지 존재 여부")
                        )
                ));
    }
}
