package com.example.lolserver.docs.controller;

import com.example.lolserver.controller.duo.DuoPostController;
import com.example.lolserver.controller.duo.request.CreateDuoPostRequest;
import com.example.lolserver.controller.duo.request.UpdateDuoPostRequest;
import com.example.lolserver.docs.RestDocsSupport;
import com.example.lolserver.docs.TestAuthenticatedMemberResolver;
import com.example.lolserver.domain.duo.application.model.DuoPostDetailReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostListReadModel;
import com.example.lolserver.domain.duo.application.model.DuoPostReadModel;
import com.example.lolserver.domain.duo.application.model.DuoRequestReadModel;
import com.example.lolserver.domain.duo.application.port.in.DuoPostQueryUseCase;
import com.example.lolserver.domain.duo.application.port.in.DuoPostUseCase;
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

@DisplayName("DuoPostController 테스트")
@ExtendWith(MockitoExtension.class)
class DuoPostControllerTest extends RestDocsSupport {

    @Mock
    private DuoPostUseCase duoPostUseCase;

    @Mock
    private DuoPostQueryUseCase duoPostQueryUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object initController() {
        return new DuoPostController(duoPostUseCase, duoPostQueryUseCase);
    }

    @Override
    protected HandlerMethodArgumentResolver[] customArgumentResolvers() {
        return new HandlerMethodArgumentResolver[]{new TestAuthenticatedMemberResolver()};
    }

    private DuoPostReadModel sampleDuoPost() {
        return DuoPostReadModel.builder()
                .id(1L)
                .primaryLane("MID")
                .secondaryLane("JUNGLE")
                .hasMicrophone(true)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(50)
                .memo("듀오 구합니다")
                .status("ACTIVE")
                .tierAvailable(true)
                .expiresAt(LocalDateTime.of(2026, 4, 15, 10, 0, 0))
                .createdAt(LocalDateTime.of(2026, 4, 14, 10, 0, 0))
                .build();
    }

    private DuoPostListReadModel sampleDuoPostList(Long id) {
        return DuoPostListReadModel.builder()
                .id(id)
                .primaryLane("MID")
                .secondaryLane("JUNGLE")
                .hasMicrophone(true)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(50)
                .memo("듀오 구합니다")
                .status("ACTIVE")
                .requestCount(3)
                .expiresAt(LocalDateTime.of(2026, 4, 15, 10, 0, 0))
                .createdAt(LocalDateTime.of(2026, 4, 14, 10, 0, 0))
                .build();
    }

    private DuoPostDetailReadModel sampleDuoPostDetail() {
        return DuoPostDetailReadModel.builder()
                .id(1L)
                .primaryLane("MID")
                .secondaryLane("JUNGLE")
                .hasMicrophone(true)
                .tier("GOLD")
                .rank("I")
                .leaguePoints(50)
                .memo("듀오 구합니다")
                .status("ACTIVE")
                .isOwner(true)
                .expiresAt(LocalDateTime.of(2026, 4, 15, 10, 0, 0))
                .createdAt(LocalDateTime.of(2026, 4, 14, 10, 0, 0))
                .requests(List.of(
                        DuoRequestReadModel.builder()
                                .id(10L)
                                .duoPostId(1L)
                                .primaryLane("ADC")
                                .secondaryLane("SUPPORT")
                                .hasMicrophone(false)
                                .tier("SILVER")
                                .rank("II")
                                .leaguePoints(30)
                                .memo("같이 하실 분")
                                .status("PENDING")
                                .createdAt(LocalDateTime.of(2026, 4, 14, 11, 0, 0))
                                .build()
                ))
                .build();
    }

    @DisplayName("듀오 게시글 작성 API")
    @Test
    void createDuoPost() throws Exception {
        given(duoPostUseCase.createDuoPost(eq(1L), any())).willReturn(sampleDuoPost());

        CreateDuoPostRequest request = new CreateDuoPostRequest(
                "MID", "JUNGLE", true, "듀오 구합니다");

        mockMvc.perform(
                        post("/api/duo/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-post-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("primaryLane").type(JsonFieldType.STRING)
                                        .description("주 라인 (TOP, JUNGLE, MID, ADC, SUPPORT)"),
                                fieldWithPath("secondaryLane").type(JsonFieldType.STRING)
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
                                        .description("게시글 ID"),
                                fieldWithPath("data.primaryLane").type(JsonFieldType.STRING)
                                        .description("주 라인"),
                                fieldWithPath("data.secondaryLane").type(JsonFieldType.STRING)
                                        .description("부 라인"),
                                fieldWithPath("data.hasMicrophone").type(JsonFieldType.BOOLEAN)
                                        .description("마이크 보유 여부"),
                                fieldWithPath("data.tier").type(JsonFieldType.STRING)
                                        .description("티어 (IRON, BRONZE, SILVER, GOLD, ...)")
                                        .optional(),
                                fieldWithPath("data.rank").type(JsonFieldType.STRING)
                                        .description("랭크 (I, II, III, IV)")
                                        .optional(),
                                fieldWithPath("data.leaguePoints").type(JsonFieldType.NUMBER)
                                        .description("리그 포인트"),
                                fieldWithPath("data.memo").type(JsonFieldType.STRING)
                                        .description("메모")
                                        .optional(),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("상태 (ACTIVE, MATCHED, DELETED, EXPIRED)"),
                                fieldWithPath("data.tierAvailable").type(JsonFieldType.BOOLEAN)
                                        .description("티어 정보 존재 여부"),
                                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING)
                                        .description("만료 일시"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("작성 일시")
                        )
                ));
    }

    @DisplayName("듀오 게시글 목록 조회 API")
    @Test
    void getDuoPosts() throws Exception {
        SliceResult<DuoPostListReadModel> page = new SliceResult<>(
                List.of(sampleDuoPostList(1L), sampleDuoPostList(2L)),
                true);
        given(duoPostQueryUseCase.getDuoPosts(any())).willReturn(page);

        mockMvc.perform(
                        get("/api/duo/posts")
                                .param("lane", "MID")
                                .param("tier", "GOLD")
                                .param("page", "0")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-post-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("lane").description("라인 필터 (TOP, JUNGLE, MID, ADC, SUPPORT)").optional(),
                                parameterWithName("tier").description("티어 필터 (IRON, BRONZE, SILVER, GOLD, ...)").optional(),
                                parameterWithName("page").description("페이지 번호 (기본값: 0)").optional()
                        ),
                        responseFields(
                                fieldWithPath("result").type(JsonFieldType.STRING)
                                        .description("API 응답 결과 (SUCCESS, ERROR)"),
                                fieldWithPath("errorMessage").type(JsonFieldType.NULL)
                                        .description("에러 메시지 (정상 응답 시 null)"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.content[].primaryLane").type(JsonFieldType.STRING)
                                        .description("주 라인"),
                                fieldWithPath("data.content[].secondaryLane").type(JsonFieldType.STRING)
                                        .description("부 라인"),
                                fieldWithPath("data.content[].hasMicrophone").type(JsonFieldType.BOOLEAN)
                                        .description("마이크 보유 여부"),
                                fieldWithPath("data.content[].tier").type(JsonFieldType.STRING)
                                        .description("티어")
                                        .optional(),
                                fieldWithPath("data.content[].rank").type(JsonFieldType.STRING)
                                        .description("랭크")
                                        .optional(),
                                fieldWithPath("data.content[].leaguePoints").type(JsonFieldType.NUMBER)
                                        .description("리그 포인트"),
                                fieldWithPath("data.content[].memo").type(JsonFieldType.STRING)
                                        .description("메모")
                                        .optional(),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                        .description("상태"),
                                fieldWithPath("data.content[].requestCount").type(JsonFieldType.NUMBER)
                                        .description("받은 요청 수"),
                                fieldWithPath("data.content[].expiresAt").type(JsonFieldType.STRING)
                                        .description("만료 일시"),
                                fieldWithPath("data.content[].createdAt").type(JsonFieldType.STRING)
                                        .description("작성 일시"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN)
                                        .description("다음 페이지 존재 여부")
                        )
                ));
    }

    @DisplayName("듀오 게시글 상세 조회 API")
    @Test
    void getDuoPost() throws Exception {
        given(duoPostQueryUseCase.getDuoPost(eq(1L), any())).willReturn(sampleDuoPostDetail());

        mockMvc.perform(
                        get("/api/duo/posts/{postId}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-post-detail",
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
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.primaryLane").type(JsonFieldType.STRING)
                                        .description("주 라인"),
                                fieldWithPath("data.secondaryLane").type(JsonFieldType.STRING)
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
                                        .description("상태"),
                                fieldWithPath("data.isOwner").type(JsonFieldType.BOOLEAN)
                                        .description("현재 사용자의 게시글 소유 여부"),
                                fieldWithPath("data.expiresAt").type(JsonFieldType.STRING)
                                        .description("만료 일시"),
                                fieldWithPath("data.createdAt").type(JsonFieldType.STRING)
                                        .description("작성 일시"),
                                fieldWithPath("data.requests[].id").type(JsonFieldType.NUMBER)
                                        .description("요청 ID"),
                                fieldWithPath("data.requests[].duoPostId").type(JsonFieldType.NUMBER)
                                        .description("게시글 ID"),
                                fieldWithPath("data.requests[].primaryLane").type(JsonFieldType.STRING)
                                        .description("요청자 주 라인"),
                                fieldWithPath("data.requests[].secondaryLane").type(JsonFieldType.STRING)
                                        .description("요청자 부 라인"),
                                fieldWithPath("data.requests[].hasMicrophone").type(JsonFieldType.BOOLEAN)
                                        .description("요청자 마이크 보유 여부"),
                                fieldWithPath("data.requests[].tier").type(JsonFieldType.STRING)
                                        .description("요청자 티어")
                                        .optional(),
                                fieldWithPath("data.requests[].rank").type(JsonFieldType.STRING)
                                        .description("요청자 랭크")
                                        .optional(),
                                fieldWithPath("data.requests[].leaguePoints").type(JsonFieldType.NUMBER)
                                        .description("요청자 리그 포인트"),
                                fieldWithPath("data.requests[].memo").type(JsonFieldType.STRING)
                                        .description("요청자 메모")
                                        .optional(),
                                fieldWithPath("data.requests[].status").type(JsonFieldType.STRING)
                                        .description("요청 상태 (PENDING, ACCEPTED, CONFIRMED, REJECTED, CANCELLED)"),
                                fieldWithPath("data.requests[].createdAt").type(JsonFieldType.STRING)
                                        .description("요청 일시")
                        )
                ));
    }

    @DisplayName("듀오 게시글 수정 API")
    @Test
    void updateDuoPost() throws Exception {
        given(duoPostUseCase.updateDuoPost(eq(1L), eq(1L), any())).willReturn(sampleDuoPost());

        UpdateDuoPostRequest request = new UpdateDuoPostRequest(
                "TOP", "SUPPORT", false, "수정된 메모");

        mockMvc.perform(
                        put("/api/duo/posts/{postId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-post-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("postId").description("게시글 ID")
                        ),
                        requestFields(
                                fieldWithPath("primaryLane").type(JsonFieldType.STRING)
                                        .description("주 라인 (TOP, JUNGLE, MID, ADC, SUPPORT)"),
                                fieldWithPath("secondaryLane").type(JsonFieldType.STRING)
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
                                subsectionWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("수정된 게시글 상세 (DuoPostResponse)")
                        )
                ));
    }

    @DisplayName("듀오 게시글 삭제 API")
    @Test
    void deleteDuoPost() throws Exception {
        willDoNothing().given(duoPostUseCase).deleteDuoPost(eq(1L), eq(1L));

        mockMvc.perform(
                        delete("/api/duo/posts/{postId}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-post-delete",
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
                                fieldWithPath("data").type(JsonFieldType.NULL)
                                        .description("데이터 없음")
                        )
                ));
    }

    @DisplayName("내 듀오 게시글 목록 조회 API")
    @Test
    void getMyDuoPosts() throws Exception {
        SliceResult<DuoPostListReadModel> page = new SliceResult<>(
                List.of(sampleDuoPostList(1L)),
                false);
        given(duoPostQueryUseCase.getMyDuoPosts(eq(1L), eq(0))).willReturn(page);

        mockMvc.perform(
                        get("/api/duo/me/posts")
                                .param("page", "0")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("duo-post-my-list",
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
                                        .description("내 듀오 게시글 목록"),
                                fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN)
                                        .description("다음 페이지 존재 여부")
                        )
                ));
    }
}
